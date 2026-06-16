package com.ecommerce.stockservice.stock.controller.dto.response;

/**
 * Tek ürün/varyantın public stok durumu.
 * {@code availableQuantity} envanter sızıntısını sınırlamak için yalnızca düşük stokta ("Son X adet")
 * dolu döner; eşiğin üstünde null'dır (inStock=true, adet gizli).
 */
public record StockAvailabilityResponse(
        Long productId,
        boolean inStock,
        Integer availableQuantity
) {
}
