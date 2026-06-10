package com.ecommerce.paymentservice.payment.controller;

import com.ecommerce.paymentservice.common.constants.ApiPaths;
import com.ecommerce.paymentservice.payment.controller.dto.request.PaymentRequest;
import com.ecommerce.paymentservice.payment.controller.dto.response.PaymentResponse;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.mapper.PaymentMapper;
import com.ecommerce.paymentservice.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(ApiPaths.Payment.PAYMENT)
@RequiredArgsConstructor
@Tag(name = "Payments", description = "iyzico payment processing for subscription and product order payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    // TODO [29.12.2025 00:33]: Burada tokendan gelen claimler ile yani CurrentUser anotasyonuyla veri kontrolu yapicaz
    @Operation(summary = "Process payment", description = "Processes a payment through iyzico for either SUBSCRIPTION (initial tenant creation) or PRODUCT_ORDER type. Called internally by user-tenant-service and order-service.")
    @ApiResponse(responseCode = "200", description = "Payment processed — check 'success' field in response")
    @ApiResponse(responseCode = "400", description = "Invalid payment data")
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request){
        log.info("REQUEST İSTEKTEKİ ÖDEME TİPİ: {}", request.type());
        PaymentContext context = paymentMapper.toContext(request);
        log.info("CONTEXT İSTEKTEKİ ÖDEME TİPİ: {}", context.getType());
        Payment payment = paymentService.processPayment(context);
        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }
}