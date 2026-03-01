package com.ecommerce.usertenantservice.exception;

import com.ecommerce.common.exception.SystemException;

public class TenantCreationException extends SystemException {
    public TenantCreationException(String message) {
        super(message, "TENANT_CREATION_FAILED");
    }
}
