package com.ecommerce.usertenantservice.tenant.controller.dto.request;

import com.ecommerce.usertenantservice.tenant.constant.BusinessType;

public record UpdateTenantCriticalRequest(
        BusinessType businessType,
        String taxId
) {}
