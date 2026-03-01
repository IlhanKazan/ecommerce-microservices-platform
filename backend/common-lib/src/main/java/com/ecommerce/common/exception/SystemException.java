package com.ecommerce.common.exception;

public class SystemException extends RuntimeException {
    private final String errorCode;
    public SystemException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
