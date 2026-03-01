package com.ecommerce.usertenantservice.exception;

import com.ecommerce.common.exception.ExternalServiceException;

public class PaymentServiceUnreachableException extends ExternalServiceException {
    public PaymentServiceUnreachableException(String message) {
        super(message, "PAYMENT_SERVICE_DOWN");
    }
}
