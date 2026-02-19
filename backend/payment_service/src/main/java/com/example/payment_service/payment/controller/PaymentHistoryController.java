package com.example.payment_service.payment.controller;

import com.example.payment_service.common.constants.ApiPaths;
import com.example.payment_service.payment.controller.dto.response.PaymentHistoryResponse;
import com.example.payment_service.payment.entity.Payment;
import com.example.payment_service.payment.mapper.PaymentMapper;
import com.example.payment_service.payment.service.PaymentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.Payment.PAYMENT_HISTORY)
@RequiredArgsConstructor
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;
    private final PaymentMapper paymentMapper;

    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<PaymentHistoryResponse>> getUserPaymentHistory(
            @PathVariable Long userId,
            Pageable pageable){
        Page<Payment> payment = paymentHistoryService.getUserPaymentHistory(userId, pageable);
        Page<PaymentHistoryResponse> response = payment.map(paymentMapper::toPaymentHistoryResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<Page<PaymentHistoryResponse>> getTenantPaymentHistory(
            @PathVariable Long tenantId,
            Pageable pageable){
        Page<Payment> payment = paymentHistoryService.getTenantPaymentHistory(tenantId, pageable);
        Page<PaymentHistoryResponse> response = payment.map(paymentMapper::toPaymentHistoryResponse);
        return ResponseEntity.ok(response);
    }

}
