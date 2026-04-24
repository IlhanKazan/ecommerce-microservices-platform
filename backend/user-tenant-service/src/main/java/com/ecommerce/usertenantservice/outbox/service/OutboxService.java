package com.ecommerce.usertenantservice.outbox.service;

import com.ecommerce.usertenantservice.tenant.entity.Tenant;

public interface OutboxService {
    void publishTenantCreatedEvent(Tenant tenant);
    void publishTenantActivatedEvent(Tenant tenant);
    void publishTenantPaymentFailedEvent(Tenant tenant);
}