package com.ecommerce.paymentservice.payment.service;

import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;
import com.ecommerce.paymentservice.payment.domain.RefundResult;

import java.math.BigDecimal;

public interface PaymentService {
    Payment processPayment(PaymentContext context);
    Payment processTokenCharge(Long tenantId, String cardToken, String cardUserKey, BigDecimal amount);

    // İade/iptal — kind=CANCEL (iyzico Cancel, iptal) / REFUND (iyzico Refund, iade). amount null → tam tutar.
    // transactionId = iyzico paymentId (Order.paymentTransactionId); ödeme bununla bulunur (Payment.orderId NULL olabilir).
    RefundResult processRefund(Long orderId, String transactionId, BigDecimal amount, String kind);
}
