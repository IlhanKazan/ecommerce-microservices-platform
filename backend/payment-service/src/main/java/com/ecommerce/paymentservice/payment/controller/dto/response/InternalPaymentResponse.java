package com.ecommerce.paymentservice.payment.controller.dto.response;

import java.math.BigDecimal;

public record InternalPaymentResponse(
        Long paymentId,
        boolean success,
        String transactionId,
        String failureReason,
        BigDecimal commissionAmount
) {}
