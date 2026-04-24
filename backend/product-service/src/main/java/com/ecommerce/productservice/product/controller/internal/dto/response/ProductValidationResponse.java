package com.ecommerce.productservice.product.controller.internal.dto.response;

import java.math.BigDecimal;

public record ProductValidationResponse(
        Long id,
        Long tenantId,
        String name,
        String sku,
        BigDecimal price,
        String currency,
        boolean available
) {}