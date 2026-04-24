package com.ecommerce.usertenantservice.tenant.controller.dto.response;

public record TenantStorefrontResponse(
        Long id,
        String name,
        String businessName,
        String logoUrl,
        String description,
        String websiteUrl
) {
}
