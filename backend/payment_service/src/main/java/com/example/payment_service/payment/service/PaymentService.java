package com.example.payment_service.payment.service;

import com.example.payment_service.payment.entity.Payment;
import com.example.payment_service.payment.domain.PaymentContext;

import java.math.BigDecimal;

public interface PaymentService {
    Payment processPayment(PaymentContext context);
    Payment processRenewalPayment(Long tenantId, String cardToken, BigDecimal amount);
}
