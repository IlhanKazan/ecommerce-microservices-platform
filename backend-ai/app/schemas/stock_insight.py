"""Merchant stok insight şemaları."""
from __future__ import annotations

from pydantic import BaseModel, Field


class ProductStockSignal(BaseModel):
    product_id: int = Field(..., serialization_alias="productId")
    product_name: str | None = Field(None, serialization_alias="productName")
    units_sold: int = Field(0, serialization_alias="unitsSold")
    available_quantity: int | None = Field(None, serialization_alias="availableQuantity")
    in_stock: bool | None = Field(None, serialization_alias="inStock")
    suggestion: str | None = None  # "REORDER" | "OK" | "OVERSTOCK" gibi etiket

    model_config = {"populate_by_name": True}


class StockInsightResponse(BaseModel):
    tenant_id: int = Field(..., serialization_alias="tenantId")
    narrative: str | None = None  # LLM Türkçe genel değerlendirme
    signals: list[ProductStockSignal] = Field(default_factory=list)

    model_config = {"populate_by_name": True}
