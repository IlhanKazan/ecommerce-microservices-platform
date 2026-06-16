package com.ecommerce.productservice.product.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductResponse(
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
        String status,
        String salesStatus,
        String tenantName,
        String tenantLogoUrl,
        List<VariantResponse> variants,
        // Merchant listesi: varyantı olan ana üründe stok = variantProductIds stoklarının toplamı.
        boolean hasVariants,
        List<Long> variantProductIds,
        // Popülerlik sayaçları (merchant analitiği).
        Integer viewCount,
        Integer saleCount,
        Boolean isFeatured
) {}