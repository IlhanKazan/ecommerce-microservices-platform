package com.ecommerce.searchservice.product.consumer.dto;

public record InventoryEventPayload(
        String orderId,
        Long productId,
        Integer amount,
        String status,
        Boolean inStock
) {
}
