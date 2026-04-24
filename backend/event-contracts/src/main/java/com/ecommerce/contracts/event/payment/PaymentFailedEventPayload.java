package com.ecommerce.contracts.event.payment;

import java.math.BigDecimal;

public record PaymentFailedEventPayload(
        Long paymentId,
        Long tenantId,
        Long customerId,
        Long orderId,
        Long subscriptionId,
        BigDecimal amount,
        String paymentType,
        String failureReason,
        String failureCode
) {
}
