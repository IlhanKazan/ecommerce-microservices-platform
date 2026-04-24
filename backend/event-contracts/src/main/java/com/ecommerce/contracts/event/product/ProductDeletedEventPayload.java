package com.ecommerce.contracts.event.product;

public record ProductDeletedEventPayload(
        Long productId,
        Long tenantId
) {}