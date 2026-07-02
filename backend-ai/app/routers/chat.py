"""Chatbot router'ı — auth gerekli."""
from __future__ import annotations

import logging

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.exceptions import (
    ExternalServiceError,
    LLMError,
    llm_http_exception,
    upstream_http_exception,
)
from app.core.security import AuthUser, CurrentUser
from app.schemas.chat import ChatRequest, ChatResponse
from app.services.chat_service import handle_chat_message

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/ai/chat", tags=["AI Chat"])


@router.post("", response_model=ChatResponse)
async def chat(
    req: ChatRequest,
    user: AuthUser = CurrentUser,
    db: AsyncSession = Depends(get_db),
) -> ChatResponse:
    """Alışveriş asistanıyla sohbet. session_id verilmezse yeni oturum başlatılır."""
    try:
        return await handle_chat_message(db, user, req.message, req.session_id)
    except LLMError as exc:
        raise llm_http_exception(exc) from exc
    except ExternalServiceError as exc:
        raise upstream_http_exception(exc) from exc
