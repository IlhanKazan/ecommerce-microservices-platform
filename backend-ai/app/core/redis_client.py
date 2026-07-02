"""Redis bağlantı havuzu — chat session memory + review summary cache."""
from __future__ import annotations

import redis.asyncio as aioredis

from app.core.config import settings

_pool: aioredis.Redis | None = None


def get_redis() -> aioredis.Redis:
    """Paylaşılan async Redis client (decode_responses=True → str döner)."""
    global _pool
    if _pool is None:
        _pool = aioredis.from_url(
            settings.redis_url,
            encoding="utf-8",
            decode_responses=True,
            health_check_interval=30,
        )
    return _pool


async def close_redis() -> None:
    global _pool
    if _pool is not None:
        await _pool.aclose()
        _pool = None
