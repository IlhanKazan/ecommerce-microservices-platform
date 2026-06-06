package com.ecommerce.contracts.event.order;

import java.util.List;
import java.util.UUID;

public record OrderConfirmedEventPayload(
        Long orderId,
        UUID userId,
        Long tenantId,
        List<OrderItemSnapshotPayload> items,
        String recipientEmail
) {}
