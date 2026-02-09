package com.example.payment_service.payment.strategy;

import com.example.payment_service.payment.entity.Payment;
import com.example.payment_service.payment.constant.PaymentType;
import com.example.payment_service.payment.domain.PaymentContext;
import com.iyzipay.request.CreatePaymentRequest;

import java.math.BigDecimal;

public interface PaymentStrategy {

    boolean supports(PaymentType type);

    BigDecimal calculatePrice(Long referenceId);

    CreatePaymentRequest prepareIyzicoRequest(Payment payment, PaymentContext context);

    CreatePaymentRequest prepareRenewalRequest(Payment payment, String cardToken);
}