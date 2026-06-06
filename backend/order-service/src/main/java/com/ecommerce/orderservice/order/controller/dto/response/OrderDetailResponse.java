package com.ecommerce.orderservice.order.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        Long orderId,
        String status,
        BigDecimal totalAmount,
        String currency,
        String shippingAddressJson,
        LocalDateTime createdAt,
        List<OrderItemDetailDto> items
) {
    public record OrderItemDetailDto(
            Long productId,
            String sku,
            String productName,
            String productImageUrl,
            BigDecimal unitPrice,
            Integer quantity
    ) {}
}
