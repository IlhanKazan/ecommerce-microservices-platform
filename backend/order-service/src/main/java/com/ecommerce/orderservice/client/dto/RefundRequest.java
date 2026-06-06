package com.ecommerce.orderservice.client.dto;

public record RefundRequest(Long orderId, String transactionId) {}
