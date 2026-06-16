package com.ecommerce.stockservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * product-service'ten gelen ES stok-agregasyon grubu.
 *
 * @param searchTargetId Güncellenecek ES dokümanının id'si (varyant ise parent, değilse ürünün kendisi).
 * @param memberIds      {@code searchTargetId}'nin inStock'unu belirleyen ürün id'leri.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockGroupResponse(
        Long searchTargetId,
        List<Long> memberIds
) {
}
