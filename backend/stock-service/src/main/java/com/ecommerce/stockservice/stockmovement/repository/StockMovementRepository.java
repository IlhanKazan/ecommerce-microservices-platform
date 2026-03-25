package com.ecommerce.stockservice.stockmovement.repository;

import com.ecommerce.stockservice.stockmovement.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
}
