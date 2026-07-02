"""AI yorum özeti router'ı — public (ürün detay sayfası kartı için)."""
from __future__ import annotations

import logging

from fastapi import APIRouter, Query

from app.core.exceptions import ExternalServiceError, LLMError, llm_http_exception, upstream_http_exception
from app.schemas.review import ReviewSummaryResponse
from app.services.review_summary_service import summarize_product_reviews

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/public/ai/reviews", tags=["AI Reviews"])


@router.get("/{product_id}/summary", response_model=ReviewSummaryResponse)
async def get_review_summary(
    product_id: int,
    refresh: bool = Query(False, description="Cache'i atla, yeniden üret"),
) -> ReviewSummaryResponse:
    """Ürün yorumlarının AI özeti. Cache varsa onu, yoksa LLM ile üretip döner."""
    try:
        return await summarize_product_reviews(
            product_id, use_cache=not refresh
        )
    except LLMError as exc:
        raise llm_http_exception(exc) from exc
    except ExternalServiceError as exc:
        raise upstream_http_exception(exc) from exc
