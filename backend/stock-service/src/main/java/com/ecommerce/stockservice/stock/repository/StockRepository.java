package com.ecommerce.stockservice.stock.repository;

import com.ecommerce.stockservice.stock.entity.Stock;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    // MANUEL İŞLEMLER VE OKUMALAR İÇİN (Pessimistic Lock yok, sadece Optimistic @Version koruması var)
    Optional<Stock> findByTenantIdAndWarehouseIdAndProductId(Long tenantId, Long warehouseId, Long productId);

    // Tenant'ın tüm stok kayıtları — JOIN FETCH ile N+1 önlendi
    @Query("SELECT s FROM Stock s JOIN FETCH s.warehouse WHERE s.tenantId = :tenantId")
    List<Stock> findAllByTenantIdWithWarehouse(@Param("tenantId") Long tenantId);

    // SADECE SAGA/SİPARİŞ İÇİN
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            // Eğer kilitliyse 3 saniye bekle, açılamazsa LockTimeoutException fırlat, bu sayede sistem sonsuz döngüde asılı (deadlock) kalmaz.
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    Optional<Stock> findWithLockingByTenantIdAndWarehouseIdAndProductId(Long tenantId, Long warehouseId, Long productId);

    // INTERNAL: Otomatik warehouse seçimi — yeterli stoğa sahip kaydı döndürür (en yüksek qty önce)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT s FROM Stock s WHERE s.tenantId = :tenantId AND s.productId = :productId " +
           "AND s.availableQuantity >= :amount ORDER BY s.availableQuantity DESC")
    List<Stock> findWithSufficientStockLocked(
            @Param("tenantId") Long tenantId,
            @Param("productId") Long productId,
            @Param("amount") int amount);

    // INTERNAL: Rollback için — ürünün tüm stok kayıtlarını döndürür
    List<Stock> findAllByTenantIdAndProductId(Long tenantId, Long productId);

    // RESYNC: availableQuantity > 0 olan tüm stok kayıtları — ES senkronizasyonu için
    @Query("SELECT s FROM Stock s WHERE s.availableQuantity > 0")
    List<Stock> findAllWithPositiveQuantity();

}