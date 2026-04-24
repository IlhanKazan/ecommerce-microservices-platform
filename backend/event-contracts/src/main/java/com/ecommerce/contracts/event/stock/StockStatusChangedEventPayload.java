package com.ecommerce.contracts.event.stock;

public record StockStatusChangedEventPayload(
        Long productId,
        Boolean inStock,
        String status
) {}