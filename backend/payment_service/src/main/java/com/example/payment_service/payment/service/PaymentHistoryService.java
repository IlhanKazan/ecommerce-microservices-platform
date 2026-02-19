package com.example.payment_service.payment.service;

import com.example.payment_service.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentHistoryService {
    public Page<Payment> getUserPaymentHistory(Long customerId, Pageable pageable);
    public Page<Payment> getTenantPaymentHistory(Long tenantId, Pageable pageable);
}
