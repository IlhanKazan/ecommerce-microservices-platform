package com.ecommerce.usertenantservice.tenant.controller.dto.response;

import com.ecommerce.usertenantservice.tenant.constant.BillingCycle;
import com.ecommerce.usertenantservice.tenant.constant.TenantSubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TenantSubscriptionResponse(
        String planName,
        BigDecimal feeAmount,
        BillingCycle cycleUnit,
        LocalDate nextBillingDate,
        TenantSubscriptionStatus status,
        LocalDateTime startedAt,
        LocalDateTime canceledAt,
        LocalDateTime lastSuccessfulPaymentDate,
        Integer failedPaymentCount,
        boolean autoRenew,
        String cancellationReason
) {}
