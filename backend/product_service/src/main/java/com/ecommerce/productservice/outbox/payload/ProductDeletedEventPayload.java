package com.ecommerce.productservice.outbox.payload;

public record ProductDeletedEventPayload(
        Long id,
        Long tenantId
) {
}
