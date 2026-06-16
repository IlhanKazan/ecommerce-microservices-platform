package com.ecommerce.searchservice.product.controller.dto;

import java.util.List;

/**
 * Tohum-bazlı ilgili ürün isteği. "Senin için" rail'i için frontend, kullanıcının son gezdiği +
 * favori ürün id'lerini seed olarak yollar; excludeIds (ör. satın alınanlar) sonuçtan çıkarılır.
 */
public record RelatedRequest(
        List<Long> seedIds,
        List<Long> excludeIds,
        Integer size
) {
}
