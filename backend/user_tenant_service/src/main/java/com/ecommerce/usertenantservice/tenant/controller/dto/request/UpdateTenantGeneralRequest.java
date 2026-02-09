package com.ecommerce.usertenantservice.tenant.controller.dto.request;

public record UpdateTenantGeneralRequest(
        String name,
        String businessName,
        String contactEmail,
        String contactPhone,
        String description,
        String websiteUrl
) {
}
