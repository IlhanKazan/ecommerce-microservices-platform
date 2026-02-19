package com.example.payment_service.payment.service.impl;

import com.example.payment_service.payment.entity.Payment;
import com.example.payment_service.payment.repository.PaymentRepository;
import com.example.payment_service.payment.service.PaymentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentHistoryServiceImpl implements PaymentHistoryService {

    private final PaymentRepository paymentRepository;

    @Override
    public Page<Payment> getUserPaymentHistory(Long customerId, Pageable pageable) {
        return paymentRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
    }

    @Override
    public Page<Payment> getTenantPaymentHistory(Long tenantId, Pageable pageable) {
        return paymentRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }
}
