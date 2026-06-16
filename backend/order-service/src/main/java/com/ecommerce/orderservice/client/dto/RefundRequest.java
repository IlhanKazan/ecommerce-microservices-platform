package com.ecommerce.orderservice.client.dto;

import java.math.BigDecimal;

/**
 * İade isteği. kind=CANCEL (iptal, iyzico Cancel) / REFUND (iade, iyzico Refund). amount null → tam tutar.
 * transactionId = Order.paymentTransactionId (iyzico paymentId) — ödeme bununla bulunur (Payment.orderId NULL).
 */
public record RefundRequest(Long orderId, String transactionId, BigDecimal amount, String kind) {}
