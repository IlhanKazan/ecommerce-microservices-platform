package com.ecommerce.usertenantservice.tenant.domain;

public record BuyerInfo(
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
) {
}
