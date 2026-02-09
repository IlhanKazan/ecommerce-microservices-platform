package com.example.payment_service.subscription.mapper;

import com.example.payment_service.subscription.controller.dto.response.SubscriptionPlanResponse;
import com.example.payment_service.subscription.entity.SubscriptionPlan;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SubscriptionPlanMapper {

    SubscriptionPlanResponse toResponse(SubscriptionPlan subscriptionPlan);

    List<SubscriptionPlanResponse> toResponseList(List<SubscriptionPlan> subscriptionPlanList);
}
