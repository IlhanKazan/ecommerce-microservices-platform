package com.ecommerce.usertenantservice.tenant.command;

public record AddressInfo(
        String contactName,
        String city,
        String country,
        String fullAddress,
        String zipCode
) {
}
