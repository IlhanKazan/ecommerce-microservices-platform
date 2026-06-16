package com.ecommerce.orderservice.order.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminOrderDetailResponse(
        Long orderId,
        Long tenantId,
        UUID userId,
        String buyerEmail,
        String status,
        BigDecimal totalAmount,
        String currency,
        String shippingAddressJson,
        String cancellationReason,
        LocalDateTime createdAt,
        List<OrderDetailResponse.OrderItemDetailDto> items
) {}
