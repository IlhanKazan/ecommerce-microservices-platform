package com.ecommerce.productservice.product.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Platform admin ürün listesi satırı (tüm tenant'lar).
 */
public record AdminProductSummaryResponse(
        Long id,
        Long tenantId,
        Long categoryId,
        String name,
        String sku,
        BigDecimal price,
        String currency,
        String status,
        String salesStatus,
        String mainImageUrl,
        LocalDateTime createdAt
) {}
