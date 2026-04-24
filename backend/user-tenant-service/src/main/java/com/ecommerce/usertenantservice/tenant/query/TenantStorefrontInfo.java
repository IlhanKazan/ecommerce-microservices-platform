package com.ecommerce.usertenantservice.tenant.query;

import java.io.Serializable;

public record TenantStorefrontInfo(
        Long id,
        String name,
        String businessName,
        String logoUrl,
        String description,
        String websiteUrl
) implements Serializable {
}
