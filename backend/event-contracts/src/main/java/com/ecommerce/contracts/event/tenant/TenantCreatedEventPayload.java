package com.ecommerce.contracts.event.tenant;

public record TenantCreatedEventPayload(
        Long tenantId,
        String name,
        String contactEmail,
        String status
) {
}