package com.ecommerce.paymentservice.payment.controller.dto.response;

public record PaymentResponse(
        Long id,
        boolean success,
        String message
) {
}
