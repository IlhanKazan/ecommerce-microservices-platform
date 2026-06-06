package com.ecommerce.orderservice.client.dto;

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
            String number,
            String expireMonth,
            String expireYear,
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
