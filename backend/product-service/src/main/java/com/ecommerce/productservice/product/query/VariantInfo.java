package com.ecommerce.productservice.product.query;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Bir parent ürünün varyantı (child product). Müşteri detay sayfasında varyant seçici bu listeden kurulur.
 * {@code attributes} varyant kombinasyonunu tutar (ör. {"Renk":"Siyah","Numara":"42"}).
 */
public record VariantInfo(
        Long id,
        String sku,
        String name,
        Map<String, String> attributes,
        BigDecimal price,
        BigDecimal discountedPrice,
        String currency,
        String mainImageUrl
) implements Serializable {
}
