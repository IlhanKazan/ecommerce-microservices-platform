package com.ecommerce.contracts.event.payment;

import java.math.BigDecimal;

public record PaymentSuccessEventPayload(
        Long paymentId,
        Long tenantId,
        Long customerId,
        Long orderId,
        Long subscriptionId,
        BigDecimal amount,
        String currency,
        String paymentType
) {
}
