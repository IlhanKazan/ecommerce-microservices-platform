package com.example.payment_service.subscription.repository;

import com.example.payment_service.subscription.constant.TenantSubscriptionStatus;
import com.example.payment_service.subscription.entity.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {
    Optional<TenantSubscription> findByIdAndStatus(Long id, TenantSubscriptionStatus status);
    List<TenantSubscription> findAllByNextBillingDateBeforeAndStatus(LocalDate date,TenantSubscriptionStatus status);
}
