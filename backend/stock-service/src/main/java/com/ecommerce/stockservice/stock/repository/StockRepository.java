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

    // Depo silme ön kontrolü — depoda hiç stok kaydı var mı
    boolean existsByTenantIdAndWarehouseId(Long tenantId, Long warehouseId);

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

    // RECONCILE: asılı kalmış rezervasyonu olan kayıtlar (sızıntı temizliği için)
    @Query("SELECT s FROM Stock s WHERE s.reservedQuantity > 0")
    List<Stock> findAllWithReservedQuantity();

    // DEPO DURUM GEÇİŞİ: belli bir depodaki stoğu olan kayıtlar — aktif/pasif olunca ES'e yansıtmak için
    @Query("SELECT s FROM Stock s WHERE s.tenantId = :tenantId AND s.warehouse.id = :warehouseId AND s.availableQuantity > 0")
    List<Stock> findPositiveStocksByWarehouse(@Param("tenantId") Long tenantId, @Param("warehouseId") Long warehouseId);

    // inStock AGGREGATE: ürünün herhangi bir AKTİF depoda satılabilir stoğu var mı
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Stock s " +
           "WHERE s.tenantId = :tenantId AND s.productId = :productId " +
           "AND s.availableQuantity > 0 AND s.warehouse.isActive = true")
    boolean existsAvailableInActiveWarehouse(@Param("tenantId") Long tenantId, @Param("productId") Long productId);

    // PUBLIC AVAILABILITY: birden çok ürün/varyant için aktif depolardaki toplam satılabilir stok.
    // productId global unique olduğundan tenant filtresine gerek yok. Listede dönmeyen id → stok yok.
    @Query("SELECT s.productId AS productId, SUM(s.availableQuantity) AS qty FROM Stock s " +
           "WHERE s.productId IN :ids AND s.warehouse.isActive = true GROUP BY s.productId")
    List<ProductStockSumProjection> sumAvailableByProductIds(@Param("ids") List<Long> ids);

}