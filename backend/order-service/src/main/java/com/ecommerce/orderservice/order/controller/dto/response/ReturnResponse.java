package com.ecommerce.orderservice.order.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** İade talebi — merchant/admin paneli listesi. */
public record ReturnResponse(
        Long returnId,
        Long orderId,
        Long tenantId,
        String reasonCode,
        String reason,
        String status,
        BigDecimal orderTotal,
        String buyerEmail,
        LocalDateTime createdAt
) {}
