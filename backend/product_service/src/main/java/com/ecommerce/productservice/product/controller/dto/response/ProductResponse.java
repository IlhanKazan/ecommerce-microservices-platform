package com.ecommerce.productservice.product.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductResponse(
        Long id,
        Long tenantId,
        Long categoryId,
        Long parentProductId,
        String name,
        String description,
        String sku,
        String brand,
        BigDecimal price,
        BigDecimal discountedPrice,
        String currency,
        String mainImageUrl,
        List<String> imageUrls,
        Map<String, String> attributes,
        BigDecimal ratingAverage,
        Integer reviewCount
) {}