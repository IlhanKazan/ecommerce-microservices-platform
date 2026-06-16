package com.ecommerce.stockservice.stock.repository;

/** Bir ürünün (productId) aktif depolardaki toplam satılabilir stoğu — availability sorgusu için projection. */
public interface ProductStockSumProjection {
    Long getProductId();
    Long getQty();
}
