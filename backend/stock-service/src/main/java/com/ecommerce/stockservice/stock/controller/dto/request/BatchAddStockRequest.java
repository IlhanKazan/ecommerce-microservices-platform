package com.ecommerce.stockservice.stock.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Tek depoya birden çok ürün/varyant için toplu stok girişi (varyant oluşturma akışında inline tohumlama). */
public record BatchAddStockRequest(
        @NotNull Long warehouseId,
        @NotEmpty @Valid List<Item> items
) {
    public record Item(
            @NotNull Long productId,
            @Min(1) int amount
    ) {
    }
}
