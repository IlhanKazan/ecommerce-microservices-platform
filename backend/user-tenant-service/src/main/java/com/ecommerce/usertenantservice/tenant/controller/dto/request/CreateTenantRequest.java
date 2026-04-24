package com.ecommerce.usertenantservice.tenant.controller.dto.request;

import com.ecommerce.usertenantservice.tenant.constant.BusinessType;
import com.ecommerce.usertenantservice.tenant.command.AddressInfo;
import com.ecommerce.usertenantservice.tenant.command.PaymentCardInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTenantRequest(
        @NotBlank String name,
        @NotBlank String businessName,
        String taxId,
        BusinessType businessType,
        String contactEmail,
        String contactPhone,
        String description,
        String websiteUrl,
        @NotNull Integer planId,
        Long selectedAddressId,
        AddressInfo newAddress,
        @NotNull PaymentCardInfo cardInfo
) {}