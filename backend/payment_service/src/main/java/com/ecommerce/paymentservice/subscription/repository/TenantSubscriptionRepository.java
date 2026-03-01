package com.ecommerce.paymentservice.subscription.repository;

import com.ecommerce.paymentservice.subscription.constant.TenantSubscriptionStatus;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {
    Optional<TenantSubscription> findByIdAndStatus(Long id, TenantSubscriptionStatus status);
    List<TenantSubscription> findAllByNextBillingDateBeforeAndStatus(LocalDate date,TenantSubscriptionStatus status);

    Optional<TenantSubscription> findFirstByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
