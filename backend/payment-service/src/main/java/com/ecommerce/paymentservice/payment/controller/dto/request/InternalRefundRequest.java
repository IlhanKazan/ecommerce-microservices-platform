package com.ecommerce.paymentservice.payment.controller.dto.request;

public record InternalRefundRequest(
        Long orderId,
        String transactionId
) {}
