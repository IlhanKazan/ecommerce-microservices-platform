package com.ecommerce.contracts.event.payment;

import java.time.LocalDate;

public record SubscriptionActivatedEventPayload(
        Long subscriptionId,
        Long tenantId,
        String planName,
        LocalDate nextBillingDate
) {
}
