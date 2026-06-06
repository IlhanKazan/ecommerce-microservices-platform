package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.client.dto.OrderPaymentRequest;
import com.ecommerce.orderservice.client.dto.PaymentResult;
import com.ecommerce.orderservice.client.dto.RefundRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${application.clients.payment.url}")
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments/internal/order-payment")
    PaymentResult processOrderPayment(@RequestBody OrderPaymentRequest request);

    @PostMapping("/api/v1/payments/internal/refund")
    void refundOrderPayment(@RequestBody RefundRequest request);
}
