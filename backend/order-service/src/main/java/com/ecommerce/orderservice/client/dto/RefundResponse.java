package com.ecommerce.orderservice.client.dto;

import java.math.BigDecimal;

/** payment-service iade sonucu — iade onayında başarı buradan kontrol edilir. */
public record RefundResponse(boolean success, String message, BigDecimal refundedAmount) {}
