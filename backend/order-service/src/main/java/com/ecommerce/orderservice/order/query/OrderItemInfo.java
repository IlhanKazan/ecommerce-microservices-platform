package com.ecommerce.orderservice.order.query;

import java.io.Serializable;
import java.math.BigDecimal;

public record OrderItemInfo(
        Long id,
        Long productId,
        String sku,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity
) implements Serializable {}
