package com.ecommerce.contracts.event.payment;

import java.time.LocalDate;

public record SubscriptionRenewalSuccessEventPayload(
        Long tenantId,
        String contactEmail,
        String planName,
        LocalDate nextBillingDate
) {}
