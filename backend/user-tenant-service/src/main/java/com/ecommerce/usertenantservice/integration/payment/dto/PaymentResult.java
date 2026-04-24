package com.ecommerce.usertenantservice.integration.payment.dto;

import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentResponse;

public record PaymentResult(
        boolean isSuccess,
        String errorMessage,
        PaymentResponse payload
) {
    public static PaymentResult success(PaymentResponse payload) {
        return new PaymentResult(true, null, payload);
    }

    public static PaymentResult failure(String errorMessage) {
        return new PaymentResult(false, errorMessage, null);
    }
}