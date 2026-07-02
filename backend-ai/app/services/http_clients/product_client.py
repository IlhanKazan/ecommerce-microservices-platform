"""product-service client — review okuma + AI callback'leri (ai-report, sentiment)."""
from __future__ import annotations

import logging
from typing import Any

from app.core.config import settings
from app.services.http_clients.base_client import BaseServiceClient
from app.services.http_clients.keycloak_client import keycloak_token_provider

logger = logging.getLogger(__name__)


class ProductServiceClient(BaseServiceClient):
    service_name = "product"

    def __init__(self) -> None:
        super().__init__(settings.product_service_url)

    async def get_reviews_page(
        self, product_id: int, page: int = 0, size: int = 50
    ) -> dict[str, Any]:
        """GET /api/v1/public/products/{id}/reviews — PageResponse<ReviewResponse>."""
        data = await self._get_json(
            f"/api/v1/public/products/{product_id}/reviews",
            params={"page": page, "size": size},
        )
        return data or {"content": [], "totalElements": 0, "isLast": True}

    async def get_all_reviews(
        self, product_id: int, max_reviews: int = 150
    ) -> list[dict[str, Any]]:
        """Tüm onaylı yorumları topla (özetleme için, üst sınırla)."""
        collected: list[dict[str, Any]] = []
        page = 0
        size = 50
        while len(collected) < max_reviews:
            data = await self.get_reviews_page(product_id, page=page, size=size)
            content = data.get("content") or []
            collected.extend(content)
            if data.get("isLast", True) or not content:
                break
            page += 1
        return collected[:max_reviews]

    async def get_public_product(self, product_id: int) -> dict[str, Any] | None:
        """GET /api/v1/public/products/{id} — ürün detayı."""
        return await self._get_json(f"/api/v1/public/products/{product_id}")

    async def update_ai_report(self, product_id: int, ai_review_report: str) -> None:
        """PATCH /api/v1/internal/products/{id}/ai-report — service token ile."""
        token = await keycloak_token_provider.get_service_token()
        await self._request(
            "PATCH",
            f"/api/v1/internal/products/{product_id}/ai-report",
            token=token,
            json_body={"aiReviewReport": ai_review_report},
        )

    async def update_review_sentiment(
        self,
        review_id: int,
        sentiment_label: str,
        sentiment_score: float | None,
        keywords: list[str],
    ) -> None:
        """PATCH /api/v1/internal/products/reviews/{id}/sentiment — service token ile."""
        token = await keycloak_token_provider.get_service_token()
        await self._request(
            "PATCH",
            f"/api/v1/internal/products/reviews/{review_id}/sentiment",
            token=token,
            json_body={
                "sentimentLabel": sentiment_label,
                "sentimentScore": sentiment_score,
                "keywords": keywords,
            },
        )


product_client = ProductServiceClient()
