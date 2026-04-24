package com.ecommerce.usertenantservice.tenant.domain;

import com.ecommerce.usertenantservice.tenant.constant.BusinessType;

public record TenantCreationContext(
        String name,
        String businessName,
        String taxId,
        BusinessType businessType,
        String contactEmail,
        String contactPhone,
        String description,
        String websiteUrl,
        Integer planId,
        Long selectedAddressId,
        AddressInfo newAddress,
        PaymentCardInfo cardInfo
) {}
