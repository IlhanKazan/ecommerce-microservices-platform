package com.ecommerce.paymentservice.payment.controller;

import com.ecommerce.paymentservice.common.constants.ApiPaths;
import com.ecommerce.paymentservice.payment.controller.dto.response.PaymentHistoryResponse;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.mapper.PaymentMapper;
import com.ecommerce.paymentservice.payment.service.PaymentHistoryService;
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
