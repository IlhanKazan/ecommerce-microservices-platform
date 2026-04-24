package com.ecommerce.usertenantservice.exception;

import com.ecommerce.common.exception.SystemException;

public class UnexpectedErrorException extends SystemException {
    public UnexpectedErrorException(String message) {
        super(message, "UNEXPECTED_ERROR");
    }
}
