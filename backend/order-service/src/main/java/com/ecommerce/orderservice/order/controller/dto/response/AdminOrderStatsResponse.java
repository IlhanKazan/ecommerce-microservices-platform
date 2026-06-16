package com.ecommerce.orderservice.order.controller.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record AdminOrderStatsResponse(
        long totalOrders,
        BigDecimal totalGmv,
        Map<String, Long> byStatus
) {}
