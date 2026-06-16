package com.ecommerce.productservice.product.query;

import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductInfo(
        Long id,
        Long tenantId,
        Long categoryId,
        String categoryName,
        Long parentProductId,
        String name,
        String sku,
        BigDecimal price,
        String currency,
        String mainImageUrl,
        ProductStatus status,
        SalesStatus salesStatus,
        Map<String, String> attributes,
        // Varyantı olan ana ürün: satış varyant üzerinden yürür, stok varyantların toplamıdır.
        boolean hasVariants,
        List<Long> variantProductIds,
        // Popülerlik sayaçları (merchant analitiği).
        Integer viewCount,
        Integer saleCount,
        // Öne çıkan ürün mü (merchant toggle durumu).
        Boolean isFeatured
) implements Serializable {
}
