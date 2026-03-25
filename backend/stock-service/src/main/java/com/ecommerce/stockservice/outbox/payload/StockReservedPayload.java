package com.ecommerce.stockservice.outbox.payload;

public record StockReservedPayload(
        String orderId,
        Long productId,
        Integer amount,
        String status,
        String eventType
) {
}
