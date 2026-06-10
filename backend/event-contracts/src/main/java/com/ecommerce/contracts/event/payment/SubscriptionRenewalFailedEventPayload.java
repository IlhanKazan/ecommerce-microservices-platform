package com.ecommerce.contracts.event.payment;

public record SubscriptionRenewalFailedEventPayload(
        Long tenantId,
        String contactEmail,
        String planName,
        String failureReason,
        int failedAttempts,
        boolean suspended
) {}
