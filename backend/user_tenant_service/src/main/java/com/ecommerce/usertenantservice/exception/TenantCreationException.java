package com.ecommerce.usertenantservice.exception;

public class TenantCreationException extends RuntimeException {
    public TenantCreationException(String message) {
        super(message);
    }
}
