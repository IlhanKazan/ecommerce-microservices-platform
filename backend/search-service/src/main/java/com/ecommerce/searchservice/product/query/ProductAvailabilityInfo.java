package com.ecommerce.searchservice.product.query;

/**
 * Tek ürünün fiziksel stok durumu — detay sayfası "Tükendi" göstergesi için.
 * ES'teki {@code inStock} (stock-service event'leriyle güncel) ile kartla aynı kaynak → tutarlı.
 */
public record ProductAvailabilityInfo(
        boolean inStock,
        String salesStatus
) {
}
