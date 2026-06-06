package com.ecommerce.contracts.event.order;

import java.util.UUID;

public record OrderRefundedEventPayload(
        Long orderId,
        UUID userId,
        Long tenantId,
        String reason,
        String recipientEmail
) {}
