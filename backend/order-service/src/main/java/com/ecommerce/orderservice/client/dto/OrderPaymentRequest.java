package com.ecommerce.orderservice.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record OrderPaymentRequest(
        Long orderId,
        Long tenantId,
        BigDecimal amount,
        String currency,
        PaymentCardDto cardInfo,
        BuyerDto buyer,
        AddressDto billingAddress,
        AddressDto shippingAddress,
        String subMerchantKey
) {
    public record PaymentCardDto(
            String holderName,
            @Schema(description = "16-digit card number", example = "5528790000000008")
            String number,
            @Schema(description = "Card expiry month (2 digits)", example = "12")
            String expireMonth,
            @Schema(description = "Card expiry year (4 digits)", example = "2030")
            String expireYear,
            @Schema(description = "Card security code", example = "123")
            String cvc
    ) {}

    public record BuyerDto(
            String id,
            String name,
            String surname,
            String email,
            String gsmNumber,
            String identityNumber,
            String ip,
            String city,
            String country,
            String zipCode,
            String fullAddress
    ) {}

    public record AddressDto(
            String contactName,
            String city,
            String country,
            String fullAddress,
            String zipCode
    ) {}
}
