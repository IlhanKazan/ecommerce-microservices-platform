package com.ecommerce.usertenantservice.tenant.controller.dto.response;

import com.ecommerce.usertenantservice.tenant.constant.TenantRole;
import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;

public record TenantSummaryResponse(
        Long id,
        String name,
        String businessName,
        String logoUrl,
        TenantStatus status,
        TenantRole myRole
) {
}
