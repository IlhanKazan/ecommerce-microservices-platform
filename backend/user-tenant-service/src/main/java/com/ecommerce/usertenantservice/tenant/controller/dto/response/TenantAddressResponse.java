package com.ecommerce.usertenantservice.tenant.controller.dto.response;

import com.ecommerce.usertenantservice.common.constants.AddressType;

public record TenantAddressResponse(
        Long id,
        String label,
        String recipientName,
        String line1,
        String line2,
        String city,
        String country,
        String zipCode,
        AddressType type
) {
}
