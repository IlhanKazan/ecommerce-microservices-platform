package com.example.payment_service.payment.repository;

import com.example.payment_service.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
    Page<Payment> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);
}
