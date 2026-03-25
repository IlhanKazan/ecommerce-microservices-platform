package com.ecommerce.stockservice.outbox.payload;

public record StockStatusChangedPayload(
        Long productId,
        Boolean inStock,
        String status
) {
}
