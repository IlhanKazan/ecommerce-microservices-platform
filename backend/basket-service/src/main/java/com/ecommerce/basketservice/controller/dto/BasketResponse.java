package com.ecommerce.basketservice.controller.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BasketResponse(
        UUID userId,
        List<BasketItemResponse> items,
        BigDecimal totalPrice
) {
}
