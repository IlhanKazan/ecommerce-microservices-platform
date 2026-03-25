package com.ecommerce.basketservice.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddItemRequest(
        @NotNull(message = "Ürün ID boş olamaz")
        Long productId,

        @Min(value = 1, message = "Miktar en az 1 olmalıdır")
        Integer quantity
) {}