package com.ecommerce.contracts.event.order;

import java.util.UUID;

/** Merchant/admin iade talebini reddetti → "talebiniz reddedildi" maili (not ile). */
public record OrderReturnRejectedEventPayload(
        Long orderId,
        UUID userId,
        Long tenantId,
        String note,
        String recipientEmail
) {}
