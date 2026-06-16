package com.ecommerce.contracts.event.product;

/**
 * Ürün popülerlik sayaçları (görüntülenme + satış) değiştiğinde yayınlanır.
 * search-service bunu ES dokümanına partial update olarak uygular; "popular" sıralamasını besler.
 * Additive event — yalnızca parent/standalone ürünler için yayılır (varyantlar indexlenmez).
 */
public record ProductStatsChangedEventPayload(
        Long productId,
        Integer viewCount,
        Integer saleCount
) {}
