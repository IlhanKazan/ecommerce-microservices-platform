package com.ecommerce.usertenantservice.tenant.controller.dto.response;

import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;

public record TenantStorefrontResponse(
        Long id,
        String name,
        String businessName,
        String logoUrl,
        String description,
        String websiteUrl,
        TenantStatus status,
        Boolean isVerified
) {
}
