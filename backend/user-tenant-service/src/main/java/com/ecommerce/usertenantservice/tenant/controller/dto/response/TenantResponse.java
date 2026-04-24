package com.ecommerce.usertenantservice.tenant.controller.dto.response;

import com.ecommerce.usertenantservice.tenant.constant.BusinessType;
import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;

import java.time.LocalDateTime;
import java.util.Set;

// TODO [6.02.2026 15:56]: Response, v1 için bu şekilde toplu ancak v2de bu ölçeklenebilirlik problemi ayrıştırılacak
public record TenantResponse(
        Long id,
        String name,
        TenantStatus status,
        String businessName,
        String taxId,
        BusinessType businessType,
        String contactEmail,
        String contactPhone,
        String description,
        String logoUrl,
        String websiteUrl,
        boolean isVerified,
        LocalDateTime createdAt,
        Set<TenantMemberResponse> members,
        Set<TenantAddressResponse> addresses,
        String iban,
        String taxOffice,
        String legalCompanyTitle
) {
}
