package com.ecommerce.stockservice.stock.entity;

public record StockInfo(
        Long productId,
        String sku,
        Integer availableQuantity,
        Integer reservedQuantity
){
}