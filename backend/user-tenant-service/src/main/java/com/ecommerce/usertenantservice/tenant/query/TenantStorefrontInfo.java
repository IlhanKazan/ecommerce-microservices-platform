package com.ecommerce.usertenantservice.tenant.query;

import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;

import java.io.Serializable;

public record TenantStorefrontInfo(
        Long id,
        String name,
        String businessName,
        String logoUrl,
        String description,
        String websiteUrl,
        TenantStatus status,
        Boolean isVerified
) implements Serializable {
}
