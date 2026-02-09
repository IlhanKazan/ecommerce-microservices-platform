package com.example.payment_service.payment.controller.dto.request;

import com.example.payment_service.payment.constant.PaymentType;
import com.example.payment_service.payment.domain.AddressInfo;
import com.example.payment_service.payment.domain.BuyerInfo;
import com.example.payment_service.payment.domain.PaymentCardInfo;

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
