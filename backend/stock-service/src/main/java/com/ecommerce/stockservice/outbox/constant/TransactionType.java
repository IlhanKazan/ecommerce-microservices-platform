package com.ecommerce.stockservice.outbox.constant;

public enum TransactionType {
    RESERVED_FOR_ORDER, ORDER_COMPLETED, ORDER_CANCELLED, MANUAL_ADJUSTMENT
}
