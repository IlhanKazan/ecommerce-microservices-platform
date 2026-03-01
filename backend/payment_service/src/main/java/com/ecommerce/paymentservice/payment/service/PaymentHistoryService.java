package com.ecommerce.paymentservice.payment.service;

import com.ecommerce.paymentservice.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentHistoryService {
    public Page<Payment> getUserPaymentHistory(Long customerId, Pageable pageable);
    public Page<Payment> getTenantPaymentHistory(Long tenantId, Pageable pageable);
}
