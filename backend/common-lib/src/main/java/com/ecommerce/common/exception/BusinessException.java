package com.ecommerce.common.exception;

import java.util.HashMap;
import java.util.Map;

public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public BusinessException(String message, String errorCode, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public Map<String, Object> getDetails() { return details; }
}
