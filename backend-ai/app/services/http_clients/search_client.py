"""search-service client — ürün arama + benzer ürün önerisi (public, ES tabanlı)."""
from __future__ import annotations

import logging
from typing import Any

from app.core.config import settings
from app.services.http_clients.base_client import BaseServiceClient

logger = logging.getLogger(__name__)


class SearchServiceClient(BaseServiceClient):
    service_name = "search"

    def __init__(self) -> None:
        super().__init__(settings.search_service_url)

    async def search_products(
        self,
        query: str | None = None,
        *,
        min_price: float | None = None,
        max_price: float | None = None,
        min_rating: int | None = None,
        categories: list[str] | None = None,
        page: int = 0,
        size: int = 8,
    ) -> dict[str, Any]:
        """POST /api/v1/public/search/products — filtreli arama."""
        body: dict[str, Any] = {"page": page, "size": size}
        if query:
            body["query"] = query
        if min_price is not None:
            body["minPrice"] = min_price
        if max_price is not None:
            body["maxPrice"] = max_price
        if min_rating is not None:
            body["minRating"] = min_rating
        if categories:
            body["categories"] = categories
        data = await self._post_json("/api/v1/public/search/products", json_body=body)
        return data or {"content": [], "totalElements": 0}

    async def get_similar(self, product_id: int, size: int = 8) -> list[dict[str, Any]]:
        """GET /api/v1/public/search/recommendations/similar/{id}."""
        data = await self._get_json(
            f"/api/v1/public/search/recommendations/similar/{product_id}",
            params={"size": size},
        )
        return data or []


search_client = SearchServiceClient()
