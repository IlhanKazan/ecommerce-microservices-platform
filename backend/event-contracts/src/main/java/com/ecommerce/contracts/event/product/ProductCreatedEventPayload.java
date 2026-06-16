package com.ecommerce.contracts.event.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductCreatedEventPayload(
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
        BigDecimal discountedPrice,  // additive — nullable, indirim yoksa null
        Long parentProductId         // additive — null değilse ürün bir varyanttır (child), search indexlemez
) {}