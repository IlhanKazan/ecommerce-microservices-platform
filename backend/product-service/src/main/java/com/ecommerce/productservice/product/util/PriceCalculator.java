package com.ecommerce.productservice.product.util;

import java.math.BigDecimal;

/**
 * Fiyat hesaplama yardımcıları. Geçerli bir indirim varsa indirimli fiyat,
 * aksi halde liste fiyatı "effective price" olarak kullanılır (sepet + checkout tahsilatı).
 */
public final class PriceCalculator {

    private PriceCalculator() {
    }

    /** İndirim ancak pozitif ve liste fiyatından küçükse geçerlidir; aksi halde liste fiyatı döner. */
    public static BigDecimal effectivePrice(BigDecimal price, BigDecimal discountedPrice) {
        if (price == null) return null;
        if (discountedPrice == null) return price;
        if (discountedPrice.compareTo(BigDecimal.ZERO) <= 0) return price;
        if (discountedPrice.compareTo(price) >= 0) return price;
        return discountedPrice;
    }
}
