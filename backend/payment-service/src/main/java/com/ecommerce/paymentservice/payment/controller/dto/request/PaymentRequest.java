package com.ecommerce.paymentservice.payment.controller.dto.request;

import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.domain.AddressInfo;
import com.ecommerce.paymentservice.payment.domain.BuyerInfo;
import com.ecommerce.paymentservice.payment.domain.PaymentCardInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment request for iyzico processing")
public record PaymentRequest(
        @Schema(description = "Payment type", example = "SUBSCRIPTION", allowableValues = {"SUBSCRIPTION", "PRODUCT_ORDER"})
        PaymentType type,
        @Schema(description = "Plan ID for SUBSCRIPTION type, Order ID for PRODUCT_ORDER type", example = "1")
        Long referenceId,
        Long customerId,
        Long tenantId,
        PaymentCardInfo cardInfo,
        BuyerInfo buyer,
        AddressInfo billingAddress,
        AddressInfo shippingAddress,
        String contactEmail
) {}
