package com.ecommerce.contracts.event.tenant;

public record TenantActivatedEventPayload(
        Long tenantId,
        String name,
        String contactEmail
) {
}