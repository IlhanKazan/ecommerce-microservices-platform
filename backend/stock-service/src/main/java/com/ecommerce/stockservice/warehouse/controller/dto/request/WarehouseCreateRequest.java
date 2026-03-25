package com.ecommerce.stockservice.warehouse.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WarehouseCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        String locationDetails
) {
}
