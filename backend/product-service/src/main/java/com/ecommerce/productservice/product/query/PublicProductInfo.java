package com.ecommerce.productservice.product.query;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PublicProductInfo(
        Long id,
        Long tenantId,
        Long categoryId,
        String categoryName,
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
        Integer reviewCount,
        Integer minOrderQty,
        Integer maxOrderQty,
        String status,
        String salesStatus
) implements Serializable {
}
