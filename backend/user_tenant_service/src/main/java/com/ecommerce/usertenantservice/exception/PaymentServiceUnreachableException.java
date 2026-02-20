package com.ecommerce.usertenantservice.exception;

public class PaymentServiceUnreachableException extends RuntimeException {
    public PaymentServiceUnreachableException(String message) {
        super(message);
    }
}
