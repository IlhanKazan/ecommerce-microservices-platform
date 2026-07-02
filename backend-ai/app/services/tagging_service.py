"""Ürün etiketi öneri servisi — merchant ürün eklerken arama görünürlüğü için.

title + description + category → LLM → Türkçe arama etiketleri.
"""
from __future__ import annotations

import logging

from app.core.llm.base import LLMMessage
from app.core.llm.factory import get_llm_client
from app.core.llm.gemini_client import safe_json_loads
from app.schemas.tagging import TagSuggestRequest, TagSuggestResponse

logger = logging.getLogger(__name__)

_SYSTEM_PROMPT = (
    "Sen bir e-ticaret platformunda ürünler için arama etiketleri (tag) üreten bir "
    "asistansın. Sana ürün başlığı, açıklaması ve kategorisi verilecek. Müşterilerin "
    "bu ürünü ararken kullanabileceği, Türkçe, kısa ve alakalı etiketler üret. "
    "Marka, model, renk, malzeme, kullanım amacı, hedef kitle gibi arama terimlerini düşün. "
    "SADECE şu JSON formatında yanıt ver, başka metin ekleme:\n"
    '{"tags": ["etiket1", "etiket2", ...]}\n'
    "En fazla 10 etiket üret. Her etiket 1-3 kelime olsun, küçük harfle yaz. "
    "SEO cümlesi değil, arama terimi üret."
)


async def suggest_tags(req: TagSuggestRequest) -> TagSuggestResponse:
    parts = [f"Başlık: {req.title}"]
    if req.category:
        parts.append(f"Kategori: {req.category}")
    if req.description:
        parts.append(f"Açıklama: {req.description}")
    user_content = "\n".join(parts)

    llm = get_llm_client()
    messages = [
        LLMMessage(role="system", content=_SYSTEM_PROMPT),
        LLMMessage(role="user", content=user_content),
    ]
    response = await llm.complete(messages, temperature=0.5, json_mode=True)
    parsed = safe_json_loads(response.content)

    raw_tags = parsed.get("tags") or []
    # normalize: küçük harf, trim, boşları at, tekille, üst sınır
    seen: set[str] = set()
    tags: list[str] = []
    for t in raw_tags:
        if not isinstance(t, str):
            continue
        norm = t.strip().lower()
        if norm and norm not in seen:
            seen.add(norm)
            tags.append(norm)
        if len(tags) >= 10:
            break

    return TagSuggestResponse(tags=tags)
