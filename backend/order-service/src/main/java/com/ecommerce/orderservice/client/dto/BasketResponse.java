package com.ecommerce.orderservice.client.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BasketResponse(
        UUID userId,
        List<BasketItemDto> items,
        BigDecimal totalPrice
) {
    public record BasketItemDto(
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal price,
            String imageUrl
    ) {}
}
