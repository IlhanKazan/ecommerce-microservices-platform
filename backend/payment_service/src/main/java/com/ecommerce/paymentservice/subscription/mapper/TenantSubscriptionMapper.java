package com.ecommerce.paymentservice.subscription.mapper;

import com.ecommerce.paymentservice.subscription.controller.dto.response.TenantSubscriptionResponse;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantSubscriptionMapper {
    TenantSubscriptionResponse toResponse(TenantSubscription tenantSubscription);
}
