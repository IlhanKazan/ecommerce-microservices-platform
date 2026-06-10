package com.ecommerce.contracts.event.order;

import java.time.LocalDateTime;

public record OrderDeliveredEventPayload(
        Long orderId,
        Long tenantId,
        String recipientEmail,
        LocalDateTime deliveredAt
) {}
