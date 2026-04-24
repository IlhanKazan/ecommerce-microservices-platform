package com.ecommerce.productservice.product.query;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductValidationInfo(
        Long id,
        Long tenantId,
        String sku,
        String name,
        BigDecimal price,
        String currency,
        String status,
        String salesStatus
) implements Serializable {}