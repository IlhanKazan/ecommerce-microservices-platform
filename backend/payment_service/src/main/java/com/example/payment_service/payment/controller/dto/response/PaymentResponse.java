package com.example.payment_service.payment.controller.dto.response;

public record PaymentResponse(
        Long id,
        boolean success,
        String message
) {
}
