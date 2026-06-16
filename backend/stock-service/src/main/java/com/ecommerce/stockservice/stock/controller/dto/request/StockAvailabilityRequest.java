package com.ecommerce.stockservice.stock.controller.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Public availability sorgusu — ürün/varyant id listesi için stok durumu istenir. */
public record StockAvailabilityRequest(
        @NotEmpty(message = "productIds boş olamaz")
        List<Long> productIds
) {
}
