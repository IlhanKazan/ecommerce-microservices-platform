package com.ecommerce.productservice.product.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductCreateRequest(
        @NotNull(message = "Kategori ID zorunludur") Long categoryId,
        Long parentProductId,
        @NotBlank(message = "Ürün adı boş olamaz") String name,
        String description,
        @NotBlank(message = "SKU boş olamaz") String sku,
        String brand,
        @NotNull(message = "Fiyat zorunludur") @Positive BigDecimal price,
        String currency,

        Integer weightGrams,
        String dimensionsCm,

        String mainImageUrl,
        List<String> imageUrls,
        Map<String, String> attributes,
        Integer minOrderQty,
        Integer maxOrderQty,

        List<String> tags,
        String seoTitle,
        String seoDescription,
        String seoKeywords
) {
    public ProductCreateRequest {
        if (currency == null) currency = "TRY";
        if (minOrderQty == null) minOrderQty = 1;
    }
}