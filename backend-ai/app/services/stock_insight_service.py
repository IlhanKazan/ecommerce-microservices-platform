"""Merchant stok insight servisi.

order-service analitiği (en çok satanlar) + stock-service mevcut stok →
LLM ile Türkçe stok/yeniden sipariş önerisi.

authz: kullanıcının JWT'si order-service'e forward edilir; isMember değilse
order-service 403 döner → buradan da propagate olur.
"""
from __future__ import annotations

import logging

from app.core.llm.base import LLMMessage
from app.core.llm.factory import get_llm_client
from app.schemas.stock_insight import (
    ProductStockSignal,
    StockInsightResponse,
)
from app.services.http_clients.order_client import order_client
from app.services.http_clients.stock_client import stock_client

logger = logging.getLogger(__name__)

_SYSTEM_PROMPT = (
    "Sen bir e-ticaret platformunda mağaza sahiplerine stok yönetimi tavsiyesi veren "
    "bir asistansın. Sana mağazanın en çok satan ürünleri ve bunların güncel stok "
    "miktarları verilecek. Türkçe, kısa (3-5 cümle), eyleme dönük bir değerlendirme yaz: "
    "hangi ürünler tükenmek üzere ve yeniden sipariş edilmeli, hangileri bol stoklu. "
    "Net ve profesyonel ol; sayıları kullan. SADECE düz metin yanıt ver."
)

# Stok bu eşiğin altındaysa ve satış varsa "yeniden sipariş" sinyali
_REORDER_THRESHOLD = 10


def _classify(units_sold: int, available: int | None) -> str:
    if available is None:
        return "UNKNOWN"
    if available <= _REORDER_THRESHOLD and units_sold > 0:
        return "REORDER"
    if available > _REORDER_THRESHOLD and units_sold == 0:
        return "OVERSTOCK"
    return "OK"


async def get_stock_insights(tenant_id: int, token: str) -> StockInsightResponse:
    analytics = await order_client.get_tenant_analytics(token, tenant_id)
    if not analytics:
        return StockInsightResponse(
            tenant_id=tenant_id,
            narrative="Henüz yeterli satış verisi yok.",
            signals=[],
        )

    top_products = analytics.get("topProducts") or []
    if not top_products:
        return StockInsightResponse(
            tenant_id=tenant_id,
            narrative="Henüz satış yapılmış ürün bulunmuyor.",
            signals=[],
        )

    product_ids = [p["productId"] for p in top_products if p.get("productId")]
    availability = await stock_client.get_availability(product_ids)
    avail_map = {a["productId"]: a for a in availability}

    signals: list[ProductStockSignal] = []
    for p in top_products:
        pid = p.get("productId")
        a = avail_map.get(pid, {})
        units = int(p.get("unitsSold") or 0)
        available = a.get("availableQuantity")
        signals.append(
            ProductStockSignal(
                product_id=pid,
                product_name=p.get("productName"),
                units_sold=units,
                available_quantity=available,
                in_stock=a.get("inStock"),
                suggestion=_classify(units, available),
            )
        )

    narrative = await _build_narrative(signals)
    return StockInsightResponse(
        tenant_id=tenant_id, narrative=narrative, signals=signals
    )


async def _build_narrative(signals: list[ProductStockSignal]) -> str | None:
    lines = []
    for s in signals:
        avail = s.available_quantity if s.available_quantity is not None else "bilinmiyor"
        lines.append(
            f"- {s.product_name or s.product_id}: {s.units_sold} adet satıldı, "
            f"güncel stok: {avail} ({s.suggestion})"
        )
    data_block = "\n".join(lines)

    llm = get_llm_client()
    messages = [
        LLMMessage(role="system", content=_SYSTEM_PROMPT),
        LLMMessage(role="user", content=f"Mağaza ürün verileri:\n{data_block}"),
    ]
    response = await llm.complete(messages, temperature=0.4, max_tokens=400)
    return response.content
