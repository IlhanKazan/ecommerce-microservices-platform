package com.ecommerce.basketservice.controller.dto;

import java.math.BigDecimal;

public record BasketItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal price,
        String imageUrl
) {
}
