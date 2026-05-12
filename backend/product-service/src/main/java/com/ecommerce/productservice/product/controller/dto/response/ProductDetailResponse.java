package com.ecommerce.productservice.product.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductDetailResponse(
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
        BigDecimal discountPercentage,
        BigDecimal discountedPrice,
        String currency,
        String mainImageUrl,
        List<String> imageUrls,
        Map<String, String> attributes,
        Integer weightGrams,
        String dimensionsCm,
        Integer minOrderQty,
        Integer maxOrderQty,
        List<String> tags,
        String seoTitle,
        String seoDescription,
        String seoKeywords,
        String status,
        String salesStatus,
        Boolean isFeatured,
        BigDecimal ratingAverage,
        Integer reviewCount
) {}
