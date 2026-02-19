package com.example.payment_service.payment.controller;

import com.example.payment_service.common.constants.ApiPaths;
import com.example.payment_service.payment.controller.dto.request.PaymentRequest;
import com.example.payment_service.payment.controller.dto.response.PaymentResponse;
import com.example.payment_service.payment.domain.PaymentContext;
import com.example.payment_service.payment.entity.Payment;
import com.example.payment_service.payment.mapper.PaymentMapper;
import com.example.payment_service.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.Payment.PAYMENT)
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    // TODO [29.12.2025 00:33]: Burada tokendan gelen claimler ile yani CurrentUser anotasyonuyla veri kontrolu yapicaz
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        PaymentContext context = paymentMapper.toContext(request);
        Payment payment = paymentService.processPayment(context);
        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }
}