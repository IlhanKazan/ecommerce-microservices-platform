package com.ecommerce.usertenantservice.user.controller.dto.response;

public record AddressResponse(
        int id,
        String addressType,
        String line1,
        String line2,
        String city,
        String stateProvince,
        String zipCode,
        String country,
        String recipientName,
        String phoneNumber,
        Boolean isDefault,
        String label,
        Double latitude,
        Double longitude
) {
}
