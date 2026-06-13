package com.ecommerce.paymentservice.subscription.repository;

import com.ecommerce.paymentservice.subscription.entity.TenantCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantCardRepository extends JpaRepository<TenantCard, Long> {

    List<TenantCard> findAllByTenantIdOrderByIsDefaultDescCreatedAtDesc(Long tenantId);

    Optional<TenantCard> findByTenantIdAndIsDefaultTrue(Long tenantId);

    Optional<TenantCard> findByIdAndTenantId(Long id, Long tenantId);

    Optional<TenantCard> findFirstByTenantId(Long tenantId);

    long countByTenantId(Long tenantId);
}
