"""Chatbot tool tanımları + execution dispatcher.

LLM bu tool'ları çağırabilir; biz Spring servislerine gidip sonucu modele geri veririz.
search_products ve get_product_info public; get_order_details kullanıcı JWT'siyle çalışır.
"""
from __future__ import annotations

import json
import logging
from typing import Any

from app.core.llm.base import ToolSpec
from app.services.http_clients.order_client import order_client
from app.services.http_clients.product_client import product_client
from app.services.http_clients.search_client import search_client

logger = logging.getLogger(__name__)


TOOL_SPECS: list[ToolSpec] = [
    ToolSpec(
        name="search_products",
        description=(
            "Kullanıcı bir ürün aradığında veya öneri istediğinde platform kataloğunda "
            "arama yapar. Anahtar kelime, fiyat aralığı ve minimum puanla filtrelenebilir."
        ),
        parameters={
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "Arama terimi, örn. 'koşu ayakkabısı'",
                },
                "min_price": {"type": "number", "description": "Minimum fiyat (TL)"},
                "max_price": {"type": "number", "description": "Maksimum fiyat (TL)"},
                "min_rating": {
                    "type": "integer",
                    "description": "Minimum yıldız puanı (1-5)",
                },
            },
            "required": ["query"],
        },
    ),
    ToolSpec(
        name="get_order_details",
        description=(
            "Kullanıcının belirli bir siparişinin durumunu ve içeriğini getirir. "
            "Kullanıcı 'siparişim nerede', 'X numaralı sipariş' gibi sorduğunda kullan."
        ),
        parameters={
            "type": "object",
            "properties": {
                "order_id": {
                    "type": "integer",
                    "description": "Sipariş numarası (ID)",
                }
            },
            "required": ["order_id"],
        },
    ),
    ToolSpec(
        name="get_product_info",
        description=(
            "Belirli bir ürünün güncel detaylarını (fiyat, açıklama, puan) getirir. "
            "Kullanıcı belirli bir ürün hakkında soru sorduğunda kullan."
        ),
        parameters={
            "type": "object",
            "properties": {
                "product_id": {"type": "integer", "description": "Ürün ID'si"}
            },
            "required": ["product_id"],
        },
    ),
]


def _trim_products(data: dict[str, Any], limit: int = 6) -> list[dict[str, Any]]:
    content = (data or {}).get("content") or []
    out: list[dict[str, Any]] = []
    for p in content[:limit]:
        out.append(
            {
                "id": p.get("id") or p.get("productId"),
                "name": p.get("name"),
                "price": p.get("price"),
                "rating": p.get("ratingAverage") or p.get("rating"),
                "inStock": p.get("inStock"),
                "tenantName": p.get("tenantName"),
            }
        )
    return out


async def execute_tool(
    name: str, arguments: dict[str, Any], *, user_token: str | None
) -> str:
    """Tool'u çalıştırıp LLM'e geri verilecek JSON string sonucu döner."""
    try:
        if name == "search_products":
            data = await search_client.search_products(
                query=arguments.get("query"),
                min_price=arguments.get("min_price"),
                max_price=arguments.get("max_price"),
                min_rating=arguments.get("min_rating"),
                size=6,
            )
            result = {"products": _trim_products(data)}

        elif name == "get_order_details":
            if not user_token:
                result = {"error": "Sipariş bilgisi için giriş yapmış olmalısınız."}
            else:
                order = await order_client.get_my_order_detail(
                    user_token, int(arguments["order_id"])
                )
                result = order or {"error": "Sipariş bulunamadı."}

        elif name == "get_product_info":
            product = await product_client.get_public_product(
                int(arguments["product_id"])
            )
            if product:
                result = {
                    "id": product.get("id"),
                    "name": product.get("name"),
                    "price": product.get("price"),
                    "description": (product.get("description") or "")[:500],
                    "rating": product.get("ratingAverage"),
                    "inStock": product.get("inStock"),
                }
            else:
                result = {"error": "Ürün bulunamadı."}
        else:
            result = {"error": f"Bilinmeyen tool: {name}"}

    except Exception as exc:  # tool hatası konuşmayı bozmasın
        logger.warning("Tool '%s' hatası: %s", name, exc)
        result = {"error": "Bilgi alınamadı, lütfen tekrar deneyin."}

    return json.dumps(result, ensure_ascii=False, default=str)
