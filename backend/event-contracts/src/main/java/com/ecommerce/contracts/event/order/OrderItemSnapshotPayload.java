package com.ecommerce.contracts.event.order;

import java.math.BigDecimal;

public record OrderItemSnapshotPayload(
        Long productId,
        String sku,
        String productName,
        BigDecimal unitPrice,
        Integer quantity
) {}
