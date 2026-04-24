package com.ecommerce.usertenantservice.user.controller.dto.request;

public record UserAddressRequest(
        String addressType,
        String label,
        String recipientName,
        String phoneNumber,
        String country,
        String city,
        String stateProvince,
        String zipCode,
        String line1,
        String line2,
        Boolean isDefault
) {
}
