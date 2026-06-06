package com.ecommerce.paymentservice.payment.controller.dto.request;

import com.ecommerce.paymentservice.payment.domain.AddressInfo;
import com.ecommerce.paymentservice.payment.domain.BuyerInfo;
import com.ecommerce.paymentservice.payment.domain.PaymentCardInfo;

import java.math.BigDecimal;

public record InternalOrderPaymentRequest(
        Long orderId,
        Long tenantId,
        BigDecimal amount,
        String currency,
        PaymentCardInfo cardInfo,
        BuyerInfo buyer,
        AddressInfo billingAddress,
        AddressInfo shippingAddress,
        String subMerchantKey
) {}
