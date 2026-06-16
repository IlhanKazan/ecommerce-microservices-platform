package com.ecommerce.stockservice.stock.query;

import java.io.Serializable;

/**
 * Bir ürün/varyantın public stok durumu (service çıktısı).
 * {@code availableQuantity} yalnızca düşük stokta dolu ("Son X adet"); eşiğin üstünde null.
 */
public record StockAvailabilityInfo(
        Long productId,
        boolean inStock,
        Integer availableQuantity
) implements Serializable {
}
