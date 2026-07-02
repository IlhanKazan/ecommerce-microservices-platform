"""Gezinme/öneri analitik modelleri — view tracking + batch özet job durumu."""
from __future__ import annotations

from datetime import datetime

from sqlalchemy import BigInteger, DateTime, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class ProductViewEvent(Base):
    __tablename__ = "product_view_events"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_keycloak_id: Mapped[str | None] = mapped_column(
        String(36), nullable=True, index=True
    )
    product_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    tenant_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    viewed_at: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=func.now()
    )


class AiSummaryJob(Base):
    """Batch özetleme idempotency/durum kaydı — ürün başına bir satır."""

    __tablename__ = "ai_summary_jobs"

    product_id: Mapped[int] = mapped_column(
        BigInteger, primary_key=True, autoincrement=False
    )
    review_count_at_last_run: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0
    )
    last_run_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    status: Mapped[str] = mapped_column(String(20), nullable=False, default="PENDING")
    error_message: Mapped[str | None] = mapped_column(Text, nullable=True)
