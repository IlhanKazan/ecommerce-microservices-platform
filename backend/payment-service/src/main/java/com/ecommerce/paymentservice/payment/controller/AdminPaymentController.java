package com.ecommerce.paymentservice.payment.controller;

import com.ecommerce.paymentservice.common.constants.ApiPaths;
import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.controller.dto.response.AdminPaymentStatsResponse;
import com.ecommerce.paymentservice.payment.controller.dto.response.AdminPaymentSummaryResponse;
import com.ecommerce.paymentservice.payment.service.AdminPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.Payment.ADMIN)
@RequiredArgsConstructor
@PreAuthorize("hasRole('platform-admin')")
@Tag(name = "Admin — Payments", description = "Platform admin gelir istatistikleri + ödeme listesi (platform-admin rolü gerekir)")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @Operation(summary = "Gelir istatistikleri", description = "Komisyon + abonelik geliri toplamı, ödeme sayıları ve aylık gelir serisi (dashboard).")
    @ApiResponse(responseCode = "200", description = "Gelir özeti")
    @GetMapping("/stats")
    public ResponseEntity<AdminPaymentStatsResponse> getStats() {
        return ResponseEntity.ok(adminPaymentService.getStats());
    }

    @Operation(summary = "Ödeme listesi", description = "Sayfalı platform geneli ödemeler. Opsiyonel type + status + tenantId filtreleri.")
    @ApiResponse(responseCode = "200", description = "Ödeme sayfası")
    @GetMapping("/payments")
    public ResponseEntity<Page<AdminPaymentSummaryResponse>> listPayments(
            @RequestParam(required = false) PaymentType type,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Long tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(adminPaymentService.listPayments(type, status, tenantId, pageable));
    }
}
