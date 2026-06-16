package com.ecommerce.productservice.product.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Map;

/** Varyant oluşturma/güncelleme isteği. attributes = varyant kombinasyonu (Renk/Numara vb.). */
public record VariantRequest(
        @NotEmpty(message = "Varyant özellikleri boş olamaz")
        Map<String, String> attributes,

        @NotBlank(message = "SKU boş olamaz")
        String sku,

        @NotNull(message = "Fiyat boş olamaz")
        @Positive(message = "Fiyat 0'dan büyük olmalı")
        BigDecimal price,

        BigDecimal discountedPrice,
        String mainImageUrl,
        String name
) {
}
