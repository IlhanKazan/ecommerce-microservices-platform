package com.ecommerce.paymentservice.payment.controller;

import com.ecommerce.paymentservice.common.constants.ApiPaths;
import com.ecommerce.paymentservice.payment.controller.dto.response.PaymentHistoryResponse;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.mapper.PaymentMapper;
import com.ecommerce.paymentservice.payment.service.PaymentHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payment History", description = "Paginated payment transaction history per user or tenant")
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;
    private final PaymentMapper paymentMapper;

    @Operation(summary = "Get user payment history", description = "Paginated list of all payment transactions for a user.")
    @ApiResponse(responseCode = "200", description = "Payment history page")
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<PaymentHistoryResponse>> getUserPaymentHistory(
            @PathVariable Long userId,
            Pageable pageable){
        Page<Payment> payment = paymentHistoryService.getUserPaymentHistory(userId, pageable);
        Page<PaymentHistoryResponse> response = payment.map(paymentMapper::toPaymentHistoryResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get tenant payment history", description = "Paginated list of all payment transactions for a tenant.")
    @ApiResponse(responseCode = "200", description = "Payment history page")
    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<Page<PaymentHistoryResponse>> getTenantPaymentHistory(
            @PathVariable Long tenantId,
            Pageable pageable){
        Page<Payment> payment = paymentHistoryService.getTenantPaymentHistory(tenantId, pageable);
        Page<PaymentHistoryResponse> response = payment.map(paymentMapper::toPaymentHistoryResponse);
        return ResponseEntity.ok(response);
    }

}
