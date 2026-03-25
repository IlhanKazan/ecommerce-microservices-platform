package com.ecommerce.productservice.product.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductUpdateRequest(
        @NotNull Long categoryId,
        Long parentProductId,
        @NotBlank String name,
        String description,
        @NotBlank String sku,
        String brand,
        @NotNull @Positive BigDecimal price,
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
        String seoKeywords,
        String status,
        String salesStatus
) {}