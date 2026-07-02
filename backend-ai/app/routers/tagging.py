"""Ürün etiketi öneri router'ı — auth gerekli (merchant)."""
from __future__ import annotations

import logging

from fastapi import APIRouter

from app.core.exceptions import LLMError, llm_http_exception
from app.core.security import AuthUser, CurrentUser
from app.schemas.tagging import TagSuggestRequest, TagSuggestResponse
from app.services.tagging_service import suggest_tags

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/ai/products", tags=["AI Tagging"])


@router.post("/suggest-tags", response_model=TagSuggestResponse)
async def suggest_product_tags(
    req: TagSuggestRequest,
    user: AuthUser = CurrentUser,
) -> TagSuggestResponse:
    """Ürün bilgisinden arama etiketleri önerir. Merchant ürün formunda kullanılır."""
    try:
        return await suggest_tags(req)
    except LLMError as exc:
        raise llm_http_exception(exc) from exc
