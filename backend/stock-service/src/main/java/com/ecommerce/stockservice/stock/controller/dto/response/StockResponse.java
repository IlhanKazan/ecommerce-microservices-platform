package com.ecommerce.stockservice.stock.controller.dto.response;

public record StockResponse(
        Long productId,
        String sku,
        Integer availableQuantity
) {
}
