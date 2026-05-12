package com.ecommerce.productservice.product.query;

import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductDetailInfo(
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
        ProductStatus status,
        SalesStatus salesStatus,
        Boolean isFeatured,
        BigDecimal ratingAverage,
        Integer reviewCount
) implements Serializable {}
