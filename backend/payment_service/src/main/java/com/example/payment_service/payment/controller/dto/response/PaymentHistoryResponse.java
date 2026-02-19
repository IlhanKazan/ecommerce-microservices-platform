package com.example.payment_service.payment.controller.dto.response;

import com.example.payment_service.payment.constant.PaymentMethod;
import com.example.payment_service.payment.constant.PaymentStatus;
import com.example.payment_service.payment.constant.PaymentType;

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
