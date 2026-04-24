package com.ecommerce.contracts.event.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductUpdatedEventPayload(
        Long productId,
        Long tenantId,
        Long categoryId,
        String sku,
        String name,
        String description,
        String brand,
        BigDecimal price,
        String currency,
        String mainImageUrl,
        Map<String, String> attributes,
        List<String> tags,
        String status,
        String salesStatus
) {}