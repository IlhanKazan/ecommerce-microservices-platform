package com.ecommerce.stockservice.stock.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Ürün-bazlı düşük stok eşiği güncelleme isteği (depo+ürün). */
public record UpdateThresholdRequest(
        @NotNull Long warehouseId,
        @NotNull Long productId,
        @Min(0) int threshold
) {
}
