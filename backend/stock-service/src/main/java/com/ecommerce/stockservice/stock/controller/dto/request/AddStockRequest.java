package com.ecommerce.stockservice.stock.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddStockRequest(
        @NotNull Long warehouseId,
        @NotNull Long productId,
        @Min(1) int amount
) {
}
