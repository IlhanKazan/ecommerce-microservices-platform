package com.ecommerce.paymentservice.payment.controller;

import com.ecommerce.paymentservice.common.constants.ApiPaths;
import com.ecommerce.paymentservice.payment.controller.dto.request.PaymentRequest;
import com.ecommerce.paymentservice.payment.controller.dto.response.PaymentResponse;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.mapper.PaymentMapper;
import com.ecommerce.paymentservice.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(ApiPaths.Payment.PAYMENT)
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    // TODO [29.12.2025 00:33]: Burada tokendan gelen claimler ile yani CurrentUser anotasyonuyla veri kontrolu yapicaz
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request){
        log.info("REQUEST İSTEKTEKİ ÖDEME TİPİ: {}", request.type());
        PaymentContext context = paymentMapper.toContext(request);
        log.info("CONTEXT İSTEKTEKİ ÖDEME TİPİ: {}", context.getType());
        Payment payment = paymentService.processPayment(context);
        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }
}