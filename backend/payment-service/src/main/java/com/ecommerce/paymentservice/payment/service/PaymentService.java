package com.ecommerce.paymentservice.payment.service;

import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;

import java.math.BigDecimal;

public interface PaymentService {
    Payment processPayment(PaymentContext context);
    Payment processRenewalPayment(Long tenantId, String cardToken, BigDecimal amount);
}
