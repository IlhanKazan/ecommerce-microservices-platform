package com.ecommerce.orderservice.order.controller.dto.request;

import com.ecommerce.orderservice.client.dto.OrderPaymentRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotBlank String shippingAddressJson,
        @NotNull OrderPaymentRequest.PaymentCardDto cardInfo,
        @NotNull OrderPaymentRequest.BuyerDto buyer,
        @NotNull OrderPaymentRequest.AddressDto billingAddress
) {}
