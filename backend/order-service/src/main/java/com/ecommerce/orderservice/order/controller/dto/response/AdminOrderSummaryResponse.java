package com.ecommerce.orderservice.order.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminOrderSummaryResponse(
        Long orderId,
        Long tenantId,
        UUID userId,
        String buyerEmail,
        String status,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime createdAt
) {}
