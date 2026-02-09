package com.ecommerce.usertenantservice.tenant.domain;

public record AddressInfo(
        String contactName,
        String city,
        String country,
        String fullAddress,
        String zipCode
) {
}
