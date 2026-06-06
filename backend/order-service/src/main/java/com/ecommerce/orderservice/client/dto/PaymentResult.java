package com.ecommerce.orderservice.client.dto;

public record PaymentResult(
        Long paymentId,
        boolean success,
        String transactionId,
        String failureReason
) {}
