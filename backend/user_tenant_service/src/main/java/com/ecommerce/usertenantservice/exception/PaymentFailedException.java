package com.ecommerce.usertenantservice.exception;

public class PaymentFailedException extends RuntimeException {
    private final Long tenantId;

    public PaymentFailedException(String message, Long tenantId) {
        super(message);
        this.tenantId = tenantId;
    }

    public Long getTenantId(){
        return tenantId;
    }
}
