package com.ecommerce.orderservice.order.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        String status,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime createdAt
) {}
