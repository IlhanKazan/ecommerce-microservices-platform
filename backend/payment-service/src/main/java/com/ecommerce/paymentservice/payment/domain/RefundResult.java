package com.ecommerce.paymentservice.payment.domain;

import java.math.BigDecimal;

/** İade/iptal sonucu (service çıktısı) — order-service başarıyı buradan öğrenir. */
public record RefundResult(
        boolean success,
        String message,
        BigDecimal refundedAmount
) {}
