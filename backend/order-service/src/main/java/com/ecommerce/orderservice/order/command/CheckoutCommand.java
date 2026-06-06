package com.ecommerce.orderservice.order.command;

import com.ecommerce.orderservice.client.dto.OrderPaymentRequest;

import java.util.UUID;

public record CheckoutCommand(
        UUID userId,
        Long tenantId,
        String shippingAddressJson,
        OrderPaymentRequest.PaymentCardDto cardInfo,
        OrderPaymentRequest.BuyerDto buyer,
        OrderPaymentRequest.AddressDto billingAddress,
        String recipientEmail
) {}
