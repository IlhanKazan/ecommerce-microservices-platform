package com.example.payment_service.subscription.controller.dto.response;

import com.example.payment_service.subscription.constant.BillingCycle;
import com.example.payment_service.subscription.constant.TenantSubscriptionStatus;

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
) { }
