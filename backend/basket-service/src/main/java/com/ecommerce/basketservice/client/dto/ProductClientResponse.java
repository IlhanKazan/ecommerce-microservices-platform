package com.ecommerce.basketservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductClientResponse(
        Long id,
        String name,
        BigDecimal price,
        BigDecimal discountedPrice,
        String mainImageUrl,
        String status,
        String salesStatus,
        // Public ürün detayı: ana ürünün ACTIVE varyantları (parent ise dolu, varyant/standalone'da boş).
        List<VariantRef> variants
) {

    // Varyantı olan ana ürün doğrudan sepete eklenemez — müşteri bir varyant seçmeli.
    public boolean hasVariants() {
        return variants != null && !variants.isEmpty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VariantRef(Long id) {}

    // Sepete ve siparişe geçerli indirim varsa indirimli fiyat yansır; aksi halde liste fiyatı.
    public BigDecimal effectivePrice() {
        if (price == null) return null;
        if (discountedPrice == null) return price;
        if (discountedPrice.compareTo(BigDecimal.ZERO) <= 0) return price;
        if (discountedPrice.compareTo(price) >= 0) return price;
        return discountedPrice;
    }
}
