package com.ecommerce.usertenantservice.tenant.controller.dto.request;

import com.ecommerce.usertenantservice.tenant.constant.BusinessType;
import com.ecommerce.usertenantservice.tenant.command.AddressInfo;
import com.ecommerce.usertenantservice.tenant.command.PaymentCardInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a new merchant store and pay for subscription")
public record CreateTenantRequest(
        @Schema(description = "Store display name", example = "My Awesome Shop")
        @NotBlank String name,
        @NotBlank String businessName,
        @Schema(description = "Legal entity type", example = "INDIVIDUAL")
        BusinessType businessType,
        String taxId,
        String contactEmail,
        String contactPhone,
        String description,
        String websiteUrl,
        @Schema(description = "Subscription plan ID from GET /api/v1/subscriptions/plans", example = "1")
        @NotNull Integer planId,
        Long selectedAddressId,
        AddressInfo newAddress,
        @NotNull PaymentCardInfo cardInfo
) {}