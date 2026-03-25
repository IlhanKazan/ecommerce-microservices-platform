package com.ecommerce.stockservice.stockmovement.service.impl;

import com.ecommerce.stockservice.outbox.constant.TransactionType;
import com.ecommerce.stockservice.stock.entity.Stock;
import com.ecommerce.stockservice.stockmovement.entity.StockMovement;
import com.ecommerce.stockservice.stockmovement.repository.StockMovementRepository;
import com.ecommerce.stockservice.stockmovement.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementRepository movementRepository;

    @Override
    public void recordMovement(Stock stock, TransactionType type, String referenceId, int quantityChanged) {
        StockMovement movement = StockMovement.builder()
                .stock(stock)
                .transactionType(type)
                .referenceId(referenceId)
                .quantityChanged(quantityChanged)
                .build();
        movementRepository.save(movement);
    }

}
