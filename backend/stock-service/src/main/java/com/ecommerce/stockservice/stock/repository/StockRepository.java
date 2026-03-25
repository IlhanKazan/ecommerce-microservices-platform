package com.ecommerce.stockservice.stock.repository;

import com.ecommerce.stockservice.stock.entity.Stock;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    // MANUEL İŞLEMLER VE OKUMALAR İÇİN (Pessimistic Lock yok, sadece Optimistic @Version koruması var)
    Optional<Stock> findByTenantIdAndWarehouseIdAndProductId(Long tenantId, Long warehouseId, Long productId);

    // SADECE SAGA/SİPARİŞ İÇİN
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            // Eğer kilitliyse 3 saniye bekle, açılamazsa LockTimeoutException fırlat, bu sayede sistem sonsuz döngüde asılı (deadlock) kalmaz.
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    Optional<Stock> findWithLockingByTenantIdAndWarehouseIdAndProductId(Long tenantId, Long warehouseId, Long productId);

}