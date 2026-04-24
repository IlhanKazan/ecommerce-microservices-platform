package com.ecommerce.contracts.event.tenant;

public record TenantPaymentFailedEventPayload(
        Long tenantId,
        String name,
        String contactEmail
) {
}