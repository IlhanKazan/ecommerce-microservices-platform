package com.ecommerce.usertenantservice.tenant.controller.dto.response;

public record PaymentResponse(
        Long paymentId,
        boolean success,
        String message
) {
}
