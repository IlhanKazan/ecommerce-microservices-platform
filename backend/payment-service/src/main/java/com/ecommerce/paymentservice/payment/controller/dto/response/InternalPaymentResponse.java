package com.ecommerce.paymentservice.payment.controller.dto.response;

public record InternalPaymentResponse(
        Long paymentId,
        boolean success,
        String transactionId,
        String failureReason
) {}
