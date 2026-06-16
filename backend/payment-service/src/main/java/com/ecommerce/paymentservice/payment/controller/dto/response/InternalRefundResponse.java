package com.ecommerce.paymentservice.payment.controller.dto.response;

import java.math.BigDecimal;

/** İade/iptal sonucu — order-service iade onayında bunu kontrol eder. */
public record InternalRefundResponse(
        boolean success,
        String message,
        BigDecimal refundedAmount
) {}
