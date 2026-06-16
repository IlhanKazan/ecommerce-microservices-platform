package com.ecommerce.orderservice.client.dto;

import java.math.BigDecimal;

public record PaymentResult(
        Long paymentId,
        boolean success,
        String transactionId,
        String failureReason,
        BigDecimal commissionAmount
) {}
