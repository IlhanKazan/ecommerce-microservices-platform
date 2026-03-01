package com.ecommerce.usertenantservice.exception;

import com.ecommerce.common.exception.BusinessException;

public class VerificationException extends BusinessException {
    public VerificationException(String message) {
        super(message, "VERIFICATION_FAILED");
    }
}
