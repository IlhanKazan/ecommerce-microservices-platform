"""Chatbot orkestrasyon servisi.

- Session belleği Redis'te (TTL'li, son N mesaj)
- LLM tool calling döngüsü (max iterasyon)
- Kalıcı mesaj logu PostgreSQL (best-effort)
"""
from __future__ import annotations

import json
import logging
import uuid
from datetime import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.llm.base import LLMMessage
from app.core.llm.factory import get_llm_client
from app.core.redis_client import get_redis
from app.core.security import AuthUser
from app.models.chat import ChatMessage, ChatSession
from app.schemas.chat import ChatResponse
from app.services.chat_tools import TOOL_SPECS, execute_tool
from app.services.http_clients.order_client import order_client

logger = logging.getLogger(__name__)

_SESSION_PREFIX = "ai:chat:"
_MAX_TOOL_ITERATIONS = 3

_SYSTEM_PROMPT = (
    "Sen Ilhan e-ticaret platformunun yardımcı alışveriş asistanısın. "
    "Kullanıcılara ürün bulma, sipariş durumu sorgulama ve ürün önerilerinde yardımcı olursun. "
    "Türkçe, samimi ve kısa yanıt ver. "
    "Ürün araması veya sipariş bilgisi gerektiğinde sana verilen araçları (tool) kullan; "
    "uydurma bilgi verme. Araç sonucu boşsa kullanıcıya nazikçe bulunamadığını söyle. "
    "Ürün önerirken isim ve fiyatı belirt."
)


def _history_key(session_id: uuid.UUID) -> str:
    return f"{_SESSION_PREFIX}{session_id}"


async def _load_history(session_id: uuid.UUID) -> list[LLMMessage]:
    redis = get_redis()
    raw = await redis.lrange(_history_key(session_id), 0, -1)
    messages: list[LLMMessage] = []
    for item in raw:
        try:
            d = json.loads(item)
            messages.append(LLMMessage(role=d["role"], content=d["content"]))
        except (json.JSONDecodeError, KeyError):
            continue
    return messages


async def _append_history(
    session_id: uuid.UUID, role: str, content: str
) -> None:
    redis = get_redis()
    key = _history_key(session_id)
    await redis.rpush(key, json.dumps({"role": role, "content": content}))
    # yalnızca son N mesajı tut
    await redis.ltrim(key, -settings.chat_history_max_messages, -1)
    await redis.expire(key, settings.chat_session_ttl)


async def _build_user_context(user: AuthUser) -> str:
    """İlk mesajda kullanıcının son siparişlerini system context'e ekler."""
    try:
        orders = await order_client.get_my_orders(user.raw_token, page=0, size=5)
        content = orders.get("content") or []
        if not content:
            return f"Kullanıcı adı: {user.display_name}. Henüz siparişi yok."
        lines = [f"Kullanıcı adı: {user.display_name}. Son siparişleri:"]
        for o in content:
            lines.append(
                f"- Sipariş #{o.get('id')}: durum={o.get('status')}, "
                f"tutar={o.get('totalAmount') or o.get('grandTotal')}"
            )
        return "\n".join(lines)
    except Exception as exc:
        logger.debug("Kullanıcı context yüklenemedi: %s", exc)
        return f"Kullanıcı adı: {user.display_name}."


async def _ensure_session(
    db: AsyncSession, session_id: uuid.UUID | None, user: AuthUser
) -> tuple[uuid.UUID, bool]:
    """Session yoksa oluşturur. (session_id, is_new) döner.

    IDOR koruması: gönderilen session yalnızca SAHİBİ tarafından devam ettirilebilir.
    Başka kullanıcıya ait ya da var olmayan id için yeni (server-üretimli) oturum açılır;
    böylece kimse başkasının session UUID'siyle geçmişini okuyamaz / oturumuna yazamaz.
    """
    if session_id:
        existing = await db.get(ChatSession, session_id)
        if existing and existing.user_keycloak_id == str(user.keycloak_id):
            existing.last_activity_at = datetime.utcnow()
            return session_id, False
        # yok veya başka kullanıcıya ait → sessizce yeni oturum
    new_session = ChatSession(
        id=uuid.uuid4(),  # daima server üretir (client id seçemez)
        user_keycloak_id=str(user.keycloak_id),
    )
    db.add(new_session)
    await db.flush()
    return new_session.id, True


async def _log_messages(
    db: AsyncSession, session_id: uuid.UUID, user_msg: str, assistant_msg: str
) -> None:
    db.add(ChatMessage(session_id=session_id, role="user", content=user_msg))
    db.add(ChatMessage(session_id=session_id, role="assistant", content=assistant_msg))


async def handle_chat_message(
    db: AsyncSession,
    user: AuthUser,
    message: str,
    session_id: uuid.UUID | None,
) -> ChatResponse:
    sid, is_new = await _ensure_session(db, session_id, user)

    # Mesaj zinciri: system + (ilk mesajsa kullanıcı bağlamı) + geçmiş + yeni mesaj
    llm_messages: list[LLMMessage] = [
        LLMMessage(role="system", content=_SYSTEM_PROMPT)
    ]
    if is_new:
        context = await _build_user_context(user)
        llm_messages.append(LLMMessage(role="system", content=context))

    llm_messages.extend(await _load_history(sid))
    llm_messages.append(LLMMessage(role="user", content=message))

    llm = get_llm_client()
    used_tools: list[str] = []

    # Tool calling döngüsü
    final_text: str | None = None
    for _ in range(_MAX_TOOL_ITERATIONS):
        response = await llm.complete_with_tools(
            llm_messages, TOOL_SPECS, temperature=0.4
        )
        if not response.wants_tools:
            final_text = response.content
            break

        # assistant'ın tool çağrısı mesajını ekle
        llm_messages.append(
            LLMMessage(
                role="assistant",
                content=response.content,
                tool_calls=response.tool_calls,
            )
        )
        for tc in response.tool_calls:
            used_tools.append(tc.name)
            result = await execute_tool(
                tc.name, tc.arguments, user_token=user.raw_token
            )
            llm_messages.append(
                LLMMessage(
                    role="tool",
                    content=result,
                    tool_call_id=tc.id,
                    name=tc.name,
                )
            )
    else:
        # iterasyon limiti — son bir düz tamamlama dene
        if final_text is None:
            closing = await llm.complete(llm_messages, temperature=0.4)
            final_text = closing.content

    final_text = final_text or "Üzgünüm, şu an yanıt veremedim. Tekrar dener misiniz?"

    # Redis session belleği + kalıcı log
    await _append_history(sid, "user", message)
    await _append_history(sid, "assistant", final_text)
    try:
        await _log_messages(db, sid, message, final_text)
    except Exception as exc:
        logger.warning("Chat mesaj logu yazılamadı: %s", exc)

    return ChatResponse(
        session_id=sid,
        message=final_text,
        used_tools=list(dict.fromkeys(used_tools)),  # tekille, sıra koru
    )
