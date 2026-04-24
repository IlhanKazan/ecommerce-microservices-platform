package com.ecommerce.usertenantservice.exception;

import com.ecommerce.common.exception.BusinessException;

public class OwnerTenantException extends BusinessException {
    public OwnerTenantException(String message) {
        super(message, "NOT_OWNER");
    }
}
