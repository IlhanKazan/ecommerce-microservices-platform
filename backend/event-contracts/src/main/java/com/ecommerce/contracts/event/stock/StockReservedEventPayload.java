package com.ecommerce.contracts.event.stock;

public record StockReservedEventPayload(
        String orderId,
        Long productId,
        Integer amount,
        String status
) {}