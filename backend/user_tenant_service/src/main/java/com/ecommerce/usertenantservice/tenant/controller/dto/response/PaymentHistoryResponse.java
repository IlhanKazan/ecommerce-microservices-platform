package com.ecommerce.usertenantservice.tenant.controller.dto.response;

import com.ecommerce.usertenantservice.tenant.constant.PaymentMethod;
import com.ecommerce.usertenantservice.tenant.constant.PaymentStatus;
import com.ecommerce.usertenantservice.tenant.constant.PaymentType;

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
) {
}
