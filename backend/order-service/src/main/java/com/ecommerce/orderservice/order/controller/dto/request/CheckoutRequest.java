package com.ecommerce.orderservice.order.controller.dto.request;

import com.ecommerce.orderservice.client.dto.OrderPaymentRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Checkout request — triggers Pay-First SAGA to create an order")
public record CheckoutRequest(
        @Schema(description = "Shipping address as JSON string", example = "{\"city\":\"Istanbul\",\"country\":\"Turkey\",\"fullAddress\":\"Bağcılar Mah. No:5\",\"zipCode\":\"34200\",\"contactName\":\"Ali Yılmaz\"}")
        @NotBlank String shippingAddressJson,
        @NotNull OrderPaymentRequest.PaymentCardDto cardInfo,
        @NotNull OrderPaymentRequest.BuyerDto buyer,
        @NotNull OrderPaymentRequest.AddressDto billingAddress
) {}
