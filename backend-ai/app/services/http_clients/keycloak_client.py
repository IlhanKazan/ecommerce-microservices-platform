"""Keycloak service-account token sağlayıcı (client_credentials).

Internal callback'ler (ai-report, sentiment) kullanıcı JWT'si olmadan çağrılır;
bu token /api/v1/internal/** için "authenticated" şartını karşılar.
Token süresi dolana kadar cache'lenir.
"""
from __future__ import annotations

import logging
import time

from app.core.config import settings
from app.core.exceptions import ExternalServiceError
from app.services.http_clients.base_client import get_http_client

logger = logging.getLogger(__name__)


class KeycloakTokenProvider:
    def __init__(self) -> None:
        self._token: str | None = None
        self._expires_at: float = 0.0

    async def get_service_token(self) -> str:
        # 30sn güvenlik payı
        if self._token and time.time() < (self._expires_at - 30):
            return self._token

        client = get_http_client()
        try:
            resp = await client.post(
                settings.token_uri,
                data={
                    "grant_type": "client_credentials",
                    "client_id": settings.my_spi_client_id,
                    "client_secret": settings.my_spi_client_secret,
                },
                headers={"Content-Type": "application/x-www-form-urlencoded"},
            )
            resp.raise_for_status()
        except Exception as exc:
            raise ExternalServiceError(
                f"Keycloak service-account token alınamadı: {exc}",
                code="KEYCLOAK_TOKEN_FAILED",
            ) from exc

        payload = resp.json()
        self._token = payload["access_token"]
        self._expires_at = time.time() + int(payload.get("expires_in", 60))
        return self._token


keycloak_token_provider = KeycloakTokenProvider()
