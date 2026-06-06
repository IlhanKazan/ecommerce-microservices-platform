package com.ecommerce.contracts.event.order;

import java.util.List;
import java.util.UUID;

public record OrderCancelledEventPayload(
        Long orderId,
        UUID userId,
        Long tenantId,
        String reason,
        List<OrderItemSnapshotPayload> items,
        String recipientEmail
) {}
