"""AI Service — FastAPI giriş noktası.

Lifespan: HTTP client + scheduler başlat/kapat.
Router'lar: reviews, tagging, stock_insights, chat, tracking.
"""
from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.core.config import settings
from app.core.redis_client import close_redis
from app.jobs.scheduler import shutdown_scheduler, start_scheduler
from app.routers import chat, reviews, stock_insights, tagging, tracking
from app.services.http_clients.base_client import (
    close_http_client,
    get_http_client,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    get_http_client()  # paylaşılan httpx client'ı hazırla
    start_scheduler()
    logger.info("%s başlatıldı (port %s).", settings.service_name, settings.ai_service_port)
    try:
        yield
    finally:
        shutdown_scheduler()
        await close_http_client()
        await close_redis()
        logger.info("%s kapatıldı.", settings.service_name)


app = FastAPI(
    title="IlhanKazan AI Service",
    description="Yorum özeti, ürün etiketleme, stok insight ve alışveriş asistanı.",
    version="1.0.0",
    lifespan=lifespan,
)

# CORS api-gateway tarafından yönetilir (tüm Spring servisleri gibi).
# Servis kendi CORS header'ını EKLEMEZ — yoksa gateway'inkiyle çift header oluşur.

app.include_router(reviews.router)
app.include_router(tagging.router)
app.include_router(stock_insights.router)
app.include_router(chat.router)
app.include_router(tracking.router)


@app.get("/health", tags=["Health"])
async def health() -> dict[str, str]:
    return {"status": "UP", "service": settings.service_name}


@app.get("/", tags=["Health"])
async def root() -> dict[str, str]:
    return {"service": settings.service_name, "docs": "/docs"}
