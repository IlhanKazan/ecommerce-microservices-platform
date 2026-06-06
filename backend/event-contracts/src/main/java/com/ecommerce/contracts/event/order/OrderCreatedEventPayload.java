package com.ecommerce.contracts.event.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEventPayload(
        Long orderId,
        UUID userId,
        Long tenantId,
        List<OrderItemSnapshotPayload> items,
        BigDecimal totalAmount,
        String currency
) {}
