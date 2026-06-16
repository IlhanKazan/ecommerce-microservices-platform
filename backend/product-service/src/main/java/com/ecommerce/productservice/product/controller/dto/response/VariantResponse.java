package com.ecommerce.productservice.product.controller.dto.response;

import java.math.BigDecimal;
import java.util.Map;

/** Müşteri detay sayfasındaki varyant (child product). Alan adları VariantInfo ile birebir (MapStruct auto-map). */
public record VariantResponse(
        Long id,
        String sku,
        String name,
        Map<String, String> attributes,
        BigDecimal price,
        BigDecimal discountedPrice,
        String currency,
        String mainImageUrl
) {}
