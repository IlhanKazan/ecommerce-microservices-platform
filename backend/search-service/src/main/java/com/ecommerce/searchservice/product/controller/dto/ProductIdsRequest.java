package com.ecommerce.searchservice.product.controller.dto;

import java.util.List;

/** Id listesiyle ürün hidrasyonu (son gezilenler, birlikte alınanlar vb. rail'ler için). */
public record ProductIdsRequest(
        List<Long> ids
) {
}
