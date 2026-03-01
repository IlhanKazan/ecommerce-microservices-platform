package com.ecommerce.paymentservice.subscription.repository;

import com.ecommerce.paymentservice.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByIdAndIsActive(Long id, boolean isActive);
    List<SubscriptionPlan> findAllByIsActive(boolean isActive);
}
