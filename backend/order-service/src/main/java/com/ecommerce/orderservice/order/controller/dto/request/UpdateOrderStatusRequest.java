package com.ecommerce.orderservice.order.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(
        @NotBlank String status,
        String trackingNumber
) {}
