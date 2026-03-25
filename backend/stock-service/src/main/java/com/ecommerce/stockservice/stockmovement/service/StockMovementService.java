package com.ecommerce.stockservice.stockmovement.service;

import com.ecommerce.stockservice.outbox.constant.TransactionType;
import com.ecommerce.stockservice.stock.entity.Stock;

public interface StockMovementService {
    void recordMovement(Stock stock, TransactionType type, String referenceId, int quantityChanged);
}
