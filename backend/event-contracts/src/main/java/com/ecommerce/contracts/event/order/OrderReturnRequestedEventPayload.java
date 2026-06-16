package com.ecommerce.contracts.event.order;

import java.util.UUID;

/** Müşteri teslim edilmiş bir sipariş için iade talebi açtı → "talebiniz alındı" maili. */
public record OrderReturnRequestedEventPayload(
        Long orderId,
        UUID userId,
        Long tenantId,
        String reason,
        String recipientEmail
) {}
