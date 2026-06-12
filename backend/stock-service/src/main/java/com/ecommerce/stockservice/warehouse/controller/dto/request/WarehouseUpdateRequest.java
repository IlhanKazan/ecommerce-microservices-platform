package com.ecommerce.stockservice.warehouse.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WarehouseUpdateRequest(
        @NotBlank String name,
        String locationDetails
) {
}
