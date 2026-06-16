package com.ecommerce.paymentservice.payment.controller.dto.request;

import java.math.BigDecimal;

/**
 * İade isteği. kind=CANCEL → iyzico Cancel(paymentId) (iptal, aynı gün); kind=REFUND → Refund(paymentTransactionId, amount) (iade).
 * transactionId = iyzico paymentId (Order.paymentTransactionId) — ödeme bununla bulunur (Payment.orderId NULL olabilir).
 * amount null ise tam ödeme tutarı kullanılır.
 */
public record InternalRefundRequest(
        Long orderId,
        String transactionId,
        BigDecimal amount,
        String kind
) {}
