package com.ecommerce.usertenantservice.integration.payment.dto;

import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentResponse;

public record PaymentResult(
        boolean isSuccess,
        boolean isInfrastructureError,
        String errorMessage,
        PaymentResponse payload
) {
    public static PaymentResult success(PaymentResponse payload) {
        return new PaymentResult(true, false, null, payload);
    }

    public static PaymentResult businessFailure(String errorMessage) {
        return new PaymentResult(false, false, errorMessage, null);
    }

    public static PaymentResult infrastructureFailure(String errorMessage) {
        return new PaymentResult(false, true, errorMessage, null);
    }
}
