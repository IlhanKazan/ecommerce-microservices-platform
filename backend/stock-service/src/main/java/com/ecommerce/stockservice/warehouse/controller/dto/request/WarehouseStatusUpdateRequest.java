package com.ecommerce.stockservice.warehouse.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record WarehouseStatusUpdateRequest(
        @NotNull Boolean active
) {
}
