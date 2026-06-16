package com.ecommerce.productservice.product.query;

import java.io.Serializable;
import java.util.List;

/**
 * Bir ürünün ES stok-durumu agregasyon grubu.
 * <p>
 * Search index'inde yalnızca parent/standalone ürünler kart olarak yer alır; varyantlar (child) indexlenmez.
 * Bir varyantın stoğu değiştiğinde güncellenmesi gereken doküman parent'tır ve parent'ın {@code inStock}
 * değeri tüm aktif varyantlarının stoğunun VEYA'sıdır.
 *
 * @param searchTargetId Güncellenecek ES dokümanının id'si (varyant ise parent, değilse ürünün kendisi).
 * @param memberIds      {@code searchTargetId}'nin inStock'unu belirleyen ürün id'leri
 *                       (varyantı olan ana üründe aktif varyantlar; aksi halde ürünün kendisi).
 */
public record StockGroupInfo(
        Long searchTargetId,
        List<Long> memberIds
) implements Serializable {
}
