package com.ecommerce.orderservice.client.dto;

import java.math.BigDecimal;

public record ProductSnapshotInfo(
        Long id,
        Long tenantId,
        String sku,
        String name,
        BigDecimal price,
        String currency,
        String status,
        String salesStatus,
        String mainImageUrl
) {}
