package com.ecommerce.stockservice.stock.controller.dto.response;

public record StockSummaryResponse(
        Long productId,
        String sku,
        Long warehouseId,
        String warehouseName,
        Integer availableQuantity,
        Integer reservedQuantity
) {
}
