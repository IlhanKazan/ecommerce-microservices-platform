package com.ecommerce.usertenantservice.exception;

import com.ecommerce.common.exception.BusinessException;

import java.util.Map;

public class PaymentFailedException extends BusinessException {

    public PaymentFailedException(String message, Long tenantId) {
        super(message, "PAYMENT_FAILED", Map.of("tenantId", tenantId));
    }

}
