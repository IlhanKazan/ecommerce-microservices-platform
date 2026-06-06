package com.ecommerce.contracts.event.stock;

public record StockCommitFailedEventPayload(
        Long orderId,
        String transactionId,
        String reason
) {}
