package com.ecommerce.paymentservice.payment.domain;

public record AddressInfo(
        String contactName,
        String city,
        String country,
        String fullAddress,
        String zipCode
) {
}
