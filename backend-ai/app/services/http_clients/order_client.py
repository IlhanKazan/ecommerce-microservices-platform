"""order-service client — kullanıcı siparişleri + merchant analitik.

authz-hassas çağrılarda kullanıcının JWT'si forward edilir; order-service'in
mevcut @CurrentUser / @tenantSecurity.isMember kontrolleri kapı görevi görür.
"""
from __future__ import annotations

import logging
from typing import Any

from app.core.config import settings
from app.services.http_clients.base_client import BaseServiceClient

logger = logging.getLogger(__name__)


class OrderServiceClient(BaseServiceClient):
    service_name = "order"

    def __init__(self) -> None:
        super().__init__(settings.order_service_url)

    async def get_my_orders(
        self, token: str, page: int = 0, size: int = 10
    ) -> dict[str, Any]:
        """GET /api/v1/orders/me — kullanıcının siparişleri (JWT forward)."""
        data = await self._get_json(
            "/api/v1/orders/me",
            token=token,
            params={"page": page, "size": size},
        )
        return data or {"content": [], "totalElements": 0}

    async def get_my_order_detail(
        self, token: str, order_id: int
    ) -> dict[str, Any] | None:
        """GET /api/v1/orders/me/{orderId} — sipariş detayı (JWT forward)."""
        return await self._get_json(
            f"/api/v1/orders/me/{order_id}", token=token
        )

    async def get_tenant_analytics(
        self, token: str, tenant_id: int
    ) -> dict[str, Any] | None:
        """GET /api/v1/orders/tenants/{tenantId}/analytics — isMember gate (JWT forward)."""
        return await self._get_json(
            f"/api/v1/orders/tenants/{tenant_id}/analytics", token=token
        )


order_client = OrderServiceClient()
