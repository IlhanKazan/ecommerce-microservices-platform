package com.ecommerce.paymentservice.payment.strategy;

import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;
import com.iyzipay.request.CreatePaymentRequest;

import java.math.BigDecimal;

public interface PaymentStrategy {

    boolean supports(PaymentType type);

    BigDecimal calculatePrice(Long referenceId);

    CreatePaymentRequest prepareIyzicoRequest(Payment payment, PaymentContext context);

    CreatePaymentRequest prepareRenewalRequest(Payment payment, String cardToken);
}