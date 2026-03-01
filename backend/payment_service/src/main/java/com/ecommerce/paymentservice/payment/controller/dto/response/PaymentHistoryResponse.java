package com.ecommerce.paymentservice.payment.controller.dto.response;

import com.ecommerce.paymentservice.payment.constant.PaymentMethod;
import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.constant.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentHistoryResponse(
        Long paymentId,
        PaymentType paymentType,
        BigDecimal amount,
        String currency,
        PaymentStatus paymentStatus,
        LocalDateTime transactionDate,
        String description,
        String failureReason,
        PaymentMethod paymentMethod
) {}
