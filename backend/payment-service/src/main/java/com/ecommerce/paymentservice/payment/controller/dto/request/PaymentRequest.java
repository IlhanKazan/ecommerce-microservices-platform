package com.ecommerce.paymentservice.payment.controller.dto.request;

import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.domain.AddressInfo;
import com.ecommerce.paymentservice.payment.domain.BuyerInfo;
import com.ecommerce.paymentservice.payment.domain.PaymentCardInfo;

public record PaymentRequest(
        PaymentType type,
        Long referenceId,
        Long customerId,
        Long tenantId,
        PaymentCardInfo cardInfo,
        BuyerInfo buyer,
        AddressInfo billingAddress,
        AddressInfo shippingAddress
) {}
