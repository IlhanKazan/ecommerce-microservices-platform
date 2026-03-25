package com.ecommerce.productservice.product.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PublicProductInfo(
        Long id,
        Long tenantId,
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
        Integer reviewCount,
        Integer minOrderQty,
        Integer maxOrderQty
) {
}
