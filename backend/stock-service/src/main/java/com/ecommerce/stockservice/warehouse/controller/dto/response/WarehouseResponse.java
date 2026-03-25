package com.ecommerce.stockservice.warehouse.controller.dto.response;

import java.time.LocalDateTime;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        String locationDetails,
        Boolean isActive,
        LocalDateTime createdAt
) {
}
