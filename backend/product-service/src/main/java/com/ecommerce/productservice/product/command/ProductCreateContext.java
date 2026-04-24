package com.ecommerce.productservice.product.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductCreateContext(
        Long tenantId,
        UUID keycloakId,
        Long categoryId,
        Long parentProductId,
        String name,
        String description,
        String sku,
        String brand,
        BigDecimal price,
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
) {}