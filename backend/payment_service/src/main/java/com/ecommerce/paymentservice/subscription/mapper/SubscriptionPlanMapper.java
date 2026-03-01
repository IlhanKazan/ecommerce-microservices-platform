package com.ecommerce.paymentservice.subscription.mapper;

import com.ecommerce.paymentservice.subscription.controller.dto.response.SubscriptionPlanResponse;
import com.ecommerce.paymentservice.subscription.entity.SubscriptionPlan;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SubscriptionPlanMapper {

    SubscriptionPlanResponse toResponse(SubscriptionPlan subscriptionPlan);

    List<SubscriptionPlanResponse> toResponseList(List<SubscriptionPlan> subscriptionPlanList);
}
