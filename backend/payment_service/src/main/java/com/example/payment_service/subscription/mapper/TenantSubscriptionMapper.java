package com.example.payment_service.subscription.mapper;

import com.example.payment_service.subscription.controller.dto.response.TenantSubscriptionResponse;
import com.example.payment_service.subscription.entity.TenantSubscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantSubscriptionMapper {
    TenantSubscriptionResponse toResponse(TenantSubscription tenantSubscription);
}
