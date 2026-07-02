"""stock-service client — toplu stok durumu (public availability)."""
from __future__ import annotations

import logging
from typing import Any

from app.core.config import settings
from app.services.http_clients.base_client import BaseServiceClient

logger = logging.getLogger(__name__)


class StockServiceClient(BaseServiceClient):
    service_name = "stock"

    def __init__(self) -> None:
        super().__init__(settings.stock_service_url)

    async def get_availability(
        self, product_ids: list[int]
    ) -> list[dict[str, Any]]:
        """POST /api/v1/public/stocks/availability — [{productId, inStock, availableQuantity}]."""
        if not product_ids:
            return []
        data = await self._post_json(
            "/api/v1/public/stocks/availability",
            json_body={"productIds": product_ids},
        )
        return data or []


stock_client = StockServiceClient()
