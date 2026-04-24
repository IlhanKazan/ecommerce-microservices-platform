package com.ecommerce.usertenantservice.tenant.messaging.event;

import java.util.UUID;

public record PaymentSuccessEvent(
        UUID tenantId
) {
}
