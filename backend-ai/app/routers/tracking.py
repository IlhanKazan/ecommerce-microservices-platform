"""Ürün görüntüleme takibi — öneri/geçmiş için view event kaydı.

Auth opsiyonel: giriş yapmış kullanıcıda keycloak_id ile, anonimde NULL ile kaydedilir.
"""
from __future__ import annotations

import logging

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import AuthUser, OptionalUser
from app.models.analytics import ProductViewEvent

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/public/ai/track", tags=["AI Tracking"])


class TrackViewRequest(BaseModel):
    product_id: int = Field(..., alias="productId")
    tenant_id: int = Field(..., alias="tenantId")

    model_config = {"populate_by_name": True}


@router.post("/view", status_code=202)
async def track_view(
    req: TrackViewRequest,
    user: AuthUser | None = OptionalUser,
    db: AsyncSession = Depends(get_db),
) -> dict[str, str]:
    """Ürün görüntüleme olayını kaydeder (best-effort, fire-and-forget)."""
    event = ProductViewEvent(
        user_keycloak_id=str(user.keycloak_id) if user else None,
        product_id=req.product_id,
        tenant_id=req.tenant_id,
    )
    db.add(event)
    return {"status": "accepted"}
