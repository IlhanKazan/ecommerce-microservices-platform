"""Downstream Spring servisleri için ortak httpx HTTP client temeli.

- Paylaşılan AsyncClient (connection pooling)
- Timeout + basit retry (geçici ağ/5xx hataları)
- JWT forward (kullanıcı token'ı) yardımcıları
"""
from __future__ import annotations

import asyncio
import logging
from typing import Any

import httpx

from app.core.exceptions import ExternalServiceError

logger = logging.getLogger(__name__)

_DEFAULT_TIMEOUT = httpx.Timeout(10.0, connect=5.0)
_MAX_RETRIES = 2
_RETRY_STATUS = {502, 503, 504}

# Lifespan boyunca paylaşılan tek client
_client: httpx.AsyncClient | None = None


def get_http_client() -> httpx.AsyncClient:
    global _client
    if _client is None:
        _client = httpx.AsyncClient(
            timeout=_DEFAULT_TIMEOUT,
            limits=httpx.Limits(max_connections=50, max_keepalive_connections=20),
        )
    return _client


async def close_http_client() -> None:
    global _client
    if _client is not None:
        await _client.aclose()
        _client = None


def bearer_headers(token: str | None) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"} if token else {}


class BaseServiceClient:
    """Servis client'larının türeyeceği temel — retry'lı request sarmalayıcı."""

    service_name: str = "downstream"

    def __init__(self, base_url: str) -> None:
        self._base_url = base_url.rstrip("/")

    async def _request(
        self,
        method: str,
        path: str,
        *,
        token: str | None = None,
        params: dict[str, Any] | None = None,
        json_body: Any | None = None,
        headers: dict[str, str] | None = None,
    ) -> httpx.Response:
        url = f"{self._base_url}{path}"
        merged_headers = bearer_headers(token)
        if headers:
            merged_headers.update(headers)

        client = get_http_client()
        last_exc: Exception | None = None

        for attempt in range(_MAX_RETRIES + 1):
            try:
                resp = await client.request(
                    method,
                    url,
                    params=params,
                    json=json_body,
                    headers=merged_headers,
                )
                if resp.status_code in _RETRY_STATUS and attempt < _MAX_RETRIES:
                    await asyncio.sleep(0.3 * (attempt + 1))
                    continue
                return resp
            except (httpx.ConnectError, httpx.ReadTimeout, httpx.ConnectTimeout) as exc:
                last_exc = exc
                if attempt < _MAX_RETRIES:
                    await asyncio.sleep(0.3 * (attempt + 1))
                    continue

        raise ExternalServiceError(
            f"{self.service_name} servisine ulaşılamadı: {last_exc}",
            code=f"{self.service_name.upper()}_UNAVAILABLE",
        )

    async def _get_json(self, path: str, **kwargs: Any) -> Any:
        resp = await self._request("GET", path, **kwargs)
        if resp.status_code == 404:
            return None
        resp.raise_for_status()
        return resp.json()

    async def _post_json(self, path: str, **kwargs: Any) -> Any:
        resp = await self._request("POST", path, **kwargs)
        resp.raise_for_status()
        if resp.content:
            return resp.json()
        return None
