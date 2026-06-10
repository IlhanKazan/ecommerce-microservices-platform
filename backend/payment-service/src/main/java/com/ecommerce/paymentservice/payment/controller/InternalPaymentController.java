package com.ecommerce.paymentservice.payment.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.paymentservice.common.constants.ApiPaths;
import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.controller.dto.request.InternalOrderPaymentRequest;
import com.ecommerce.paymentservice.payment.controller.dto.request.InternalRefundRequest;
import com.ecommerce.paymentservice.payment.controller.dto.response.InternalPaymentResponse;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@Slf4j
@RestController
@RequestMapping(ApiPaths.Internal.INTERNAL_PAYMENT)
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/order-payment")
    public ResponseEntity<InternalPaymentResponse> processOrderPayment(
            @RequestBody InternalOrderPaymentRequest request,
            @CurrentUser AuthUser user) {

        log.info("[INTERNAL] Sipariş ödemesi. OrderID: {}, TenantID: {}, Tutar: {}",
                request.orderId(), request.tenantId(), request.amount());

        // IDOR FIX: customerId @CurrentUser'dan alınır, body'den değil.
        // NOT: Payment.customerId Long tipinde, user.keycloakId() UUID tipinde — uyumsuzluk var.
        // Geçici çözüm: PRODUCT_ORDER'da customerId şu an kullanılmıyor → tenantId geçilir.
        PaymentContext context = PaymentContext.builder()
                .type(PaymentType.PRODUCT_ORDER)
                .referenceId(request.orderId())
                .customerId(request.tenantId())
                .tenantId(request.tenantId())
                .cardInfo(request.cardInfo())
                .buyer(request.buyer())
                .billingAddress(request.billingAddress())
                .shippingAddress(request.shippingAddress())
                .amount(request.amount())
                .currency(request.currency() != null ? request.currency() : "TRY")
                .subMerchantKey(request.subMerchantKey())
                .build();

        Payment payment = paymentService.processPayment(context);

        return ResponseEntity.ok(new InternalPaymentResponse(
                payment.getId(),
                payment.getPaymentStatus() == PaymentStatus.SUCCESS,
                payment.getIyzicoTransactionId(),
                payment.getFailureReason()
        ));
    }

    @PostMapping("/refund")
    public ResponseEntity<Void> refundOrderPayment(@RequestBody InternalRefundRequest request) {
        log.info("[INTERNAL] İade isteği. OrderID: {}, TransactionID: {}",
                request.orderId(), request.transactionId());
        // TODO [01.06.2026]: Gerçek iyzico refund API çağrısı sonraki iterasyonda eklenecek
        paymentService.refundByOrderId(request.orderId(), request.transactionId());
        return ResponseEntity.noContent().build();
    }
}
