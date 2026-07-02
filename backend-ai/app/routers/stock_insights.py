"""Merchant stok insight router'ı — auth gerekli, tenant üyeliği order-service'te doğrulanır."""
from __future__ import annotations

import logging

import httpx
from fastapi import APIRouter, HTTPException, status

from app.core.exceptions import (
    ExternalServiceError,
    LLMError,
    llm_http_exception,
    upstream_http_exception,
)
from app.core.security import AuthUser, CurrentUser
from app.schemas.stock_insight import StockInsightResponse
from app.services.stock_insight_service import get_stock_insights

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/ai/merchants", tags=["AI Stock Insights"])


@router.get("/{tenant_id}/stock-insights", response_model=StockInsightResponse)
async def stock_insights(
    tenant_id: int,
    user: AuthUser = CurrentUser,
) -> StockInsightResponse:
    """Mağaza için satış + stok bazlı AI stok önerisi.

    Tenant üyeliği order-service'in @tenantSecurity.isMember kontrolüyle doğrulanır;
    üye değilse 403 propagate edilir.
    """
    try:
        return await get_stock_insights(tenant_id, user.raw_token)
    except httpx.HTTPStatusError as exc:
        # order-service'in authz reddini (403) aynen ilet
        if exc.response.status_code in (401, 403):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Bu mağazanın verilerine erişim yetkiniz yok.",
            ) from exc
        raise upstream_http_exception(
            ExternalServiceError(f"order-service hatası: {exc.response.status_code}")
        ) from exc
    except LLMError as exc:
        raise llm_http_exception(exc) from exc
    except ExternalServiceError as exc:
        raise upstream_http_exception(exc) from exc
