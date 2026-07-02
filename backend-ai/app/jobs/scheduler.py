"""APScheduler batch job'ları.

review_summary_job: son dönemde görüntülenen (popüler) ürünlerin yorumlarını
periyodik olarak özetleyip product-service'e yazar. Böylece yorum özeti anlık
LLM çağrısı yerine arka planda hazırlanır (view_count toplama mantığına benzer).
"""
from __future__ import annotations

import logging
from datetime import datetime, timedelta

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from sqlalchemy import select

from app.core.config import settings
from app.core.database import AsyncSessionLocal
from app.models.analytics import AiSummaryJob, ProductViewEvent
from app.services.review_summary_service import summarize_product_reviews

logger = logging.getLogger(__name__)

_scheduler: AsyncIOScheduler | None = None

# Bir job çalışmasında en fazla kaç ürün özetlensin (LLM maliyet koruması)
_MAX_PRODUCTS_PER_RUN = 25
# Kaç gün geriye bakıp popüler ürünleri seçelim
_LOOKBACK_DAYS = 7


async def _candidate_product_ids() -> list[int]:
    """Son _LOOKBACK_DAYS içinde görüntülenmiş distinct ürünler."""
    since = datetime.utcnow() - timedelta(days=_LOOKBACK_DAYS)
    async with AsyncSessionLocal() as db:
        stmt = (
            select(ProductViewEvent.product_id)
            .where(ProductViewEvent.viewed_at >= since)
            .distinct()
            .limit(_MAX_PRODUCTS_PER_RUN)
        )
        rows = await db.execute(stmt)
        return [r[0] for r in rows.all()]


async def _upsert_job_status(
    product_id: int, review_count: int, status: str, error: str | None
) -> None:
    async with AsyncSessionLocal() as db:
        job = await db.get(AiSummaryJob, product_id)
        if job is None:
            job = AiSummaryJob(product_id=product_id)
            db.add(job)
        job.review_count_at_last_run = review_count
        job.last_run_at = datetime.utcnow()
        job.status = status
        job.error_message = error
        await db.commit()


async def review_summary_job() -> None:
    product_ids = await _candidate_product_ids()
    if not product_ids:
        logger.info("review_summary_job: özetlenecek ürün yok.")
        return

    logger.info("review_summary_job: %d ürün işleniyor.", len(product_ids))
    for pid in product_ids:
        try:
            # cache'i atla (taze üret), product-service'e yaz
            summary = await summarize_product_reviews(
                pid, use_cache=False, persist_to_product=True
            )
            await _upsert_job_status(
                pid, summary.review_count, "PROCESSED", None
            )
        except Exception as exc:
            logger.warning("review_summary_job ürün %s başarısız: %s", pid, exc)
            await _upsert_job_status(pid, 0, "FAILED", str(exc)[:500])


def start_scheduler() -> AsyncIOScheduler:
    global _scheduler
    if _scheduler is not None:
        return _scheduler
    _scheduler = AsyncIOScheduler(timezone="Europe/Istanbul")
    _scheduler.add_job(
        review_summary_job,
        trigger="interval",
        hours=settings.review_summary_job_interval_hours,
        id="review_summary_job",
        max_instances=1,
        coalesce=True,
    )
    _scheduler.start()
    logger.info(
        "Scheduler başlatıldı — review_summary_job her %d saatte.",
        settings.review_summary_job_interval_hours,
    )
    return _scheduler


def shutdown_scheduler() -> None:
    global _scheduler
    if _scheduler is not None:
        _scheduler.shutdown(wait=False)
        _scheduler = None
