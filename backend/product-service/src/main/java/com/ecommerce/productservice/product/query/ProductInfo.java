package com.ecommerce.productservice.product.query;

import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;

import java.io.Serializable;
import java.math.BigDecimal;
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
        Map<String, String> attributes
) implements Serializable {
}
