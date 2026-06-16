package com.ecommerce.productservice.product.command;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Varyant (child product) oluşturma/güncelleme için service girdisi.
 * {@code attributes} varyant kombinasyonunu tutar (ör. {"Renk":"Siyah","Numara":"42"}).
 * Diğer alanlar (kategori, marka, açıklama, currency) parent'tan miras alınır.
 */
public record VariantCommand(
        String name,                 // opsiyonel; null → parent adı + kombinasyon
        String sku,
        BigDecimal price,
        BigDecimal discountedPrice,
        String mainImageUrl,
        Map<String, String> attributes
) {
}
