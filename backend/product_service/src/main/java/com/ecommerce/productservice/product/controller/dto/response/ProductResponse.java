package com.ecommerce.productservice.product.controller.dto.response;

import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;

import java.math.BigDecimal;
import java.util.Map;

public record ProductResponse(
        Long id,
        Long categoryId,
        Long parentProductId,
        String name,
        String sku,
        BigDecimal price,
        String currency,
        String mainImageUrl,
        ProductStatus status,
        SalesStatus salesStatus,
        Map<String, String> attributes
) {}