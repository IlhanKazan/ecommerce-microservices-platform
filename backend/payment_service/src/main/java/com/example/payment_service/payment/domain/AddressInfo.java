package com.example.payment_service.payment.domain;

public record AddressInfo(
        String contactName,
        String city,
        String country,
        String fullAddress,
        String zipCode
) {
}
