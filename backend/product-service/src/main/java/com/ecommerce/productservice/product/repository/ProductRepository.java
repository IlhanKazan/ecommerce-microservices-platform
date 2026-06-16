package com.ecommerce.productservice.product.repository;

import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;
import com.ecommerce.productservice.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>{
    Optional<Product> findByIdAndTenantId(Long id, Long tenantId);
    // Merchant ana ürün listesi — varyantlar (child) gizli; ayrı Varyantlar ekranında yönetilir.
    Page<Product> findAllByTenantIdAndStatusNotAndParentProductIsNull(Long tenantId, ProductStatus status, Pageable pageable);
    Page<Product> findByTenantIdAndCategoryIdAndStatusNot(
            Long tenantId, Long categoryId, ProductStatus status, Pageable pageable);
    // Görüntülenme sayacı: Redis tamponundan gelen delta tek batch UPDATE ile uygulanır (write-back).
    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + :delta, p.statsDirty = true WHERE p.id = :id")
    void incrementViewCountBy(@Param("id") Long id, @Param("delta") long delta);

    // Popülerlik: satış sayacı — sipariş onaylanınca artırılır.
    @Modifying
    @Query("UPDATE Product p SET p.saleCount = p.saleCount + :qty, p.statsDirty = true WHERE p.id = :id")
    void incrementSaleCount(@Param("id") Long id, @Param("qty") int qty);

    // Sold ürünün katalog karşılığı: varyantsa parent id, değilse kendisi (sale_count parent'ta toplanır).
    @Query("SELECT COALESCE(p.parentProduct.id, p.id) FROM Product p WHERE p.id = :id")
    Optional<Long> findCatalogProductId(@Param("id") Long id);

    // Popülerlik senkronu: yalnızca sayacı DEĞİŞEN (dirty) ana ürünler (varyantlar ES'te indexlenmez).
    @Query("SELECT p FROM Product p WHERE p.statsDirty = true AND p.parentProduct IS NULL")
    List<Product> findDirtyStatsProducts();

    // Senkron sonrası dirty işaretini temizle (yalnızca yayınlanan id'ler).
    @Modifying
    @Query("UPDATE Product p SET p.statsDirty = false WHERE p.id IN :ids")
    void clearStatsDirty(@Param("ids") List<Long> ids);
    List<Product> findAllByTenantId(Long tenantId);
    List<Product> findByCategoryIdAndStatus(Long categoryId, String status);
    boolean existsByCategoryId(Long categoryId);
    // Public detay: parent'ın ACTIVE varyantları
    List<Product> findByParentProductIdAndStatus(Long parentProductId, ProductStatus status);
    // Merchant Varyantlar ekranı: DELETED hariç tüm varyantlar
    List<Product> findByParentProductIdAndStatusNot(Long parentProductId, ProductStatus status);
    // Satış guard'ı: ürünün belirtilen statüde (ör. ACTIVE) varyantı var mı?
    boolean existsByParentProductIdAndStatus(Long parentProductId, ProductStatus status);
    // Satış metriği: ürünün TÜM varyant id'leri (DELETED dahil — geçmiş satış için)
    @Query("SELECT p.id FROM Product p WHERE p.parentProduct.id = :parentId")
    List<Long> findVariantIdsByParentId(@Param("parentId") Long parentId);
    // Tek ürünün belirtilen statüdeki varyant id'leri (ör. merchant listesinde stok toplamı için ACTIVE)
    @Query("SELECT p.id FROM Product p WHERE p.parentProduct.id = :parentId AND p.status = :status")
    List<Long> findVariantIdsByParentIdAndStatus(@Param("parentId") Long parentId,
                                                 @Param("status") ProductStatus status);
    // Toplu: bir sayfa parent için (parentId, variantId) satırları — N+1 yerine tek sorgu
    @Query("SELECT p.parentProduct.id, p.id FROM Product p "
            + "WHERE p.parentProduct.id IN :parentIds AND p.status = :status")
    List<Object[]> findVariantIdRowsByParentIds(@Param("parentIds") List<Long> parentIds,
                                                @Param("status") ProductStatus status);
    List<Product> findAllByStatus(ProductStatus status);

    // Platform admin: overview ürün sayımları
    long countByStatus(ProductStatus status);
    long countByStatusNot(ProductStatus status);

    // Platform admin: tüm tenant ürün listesi (DELETED hariç).
    // NOT: :q önceden hazırlanmış LIKE deseni ("%...%", lowercase) ya da null.
    // CONCAT/LOWER param üstünde çalıştırılmaz → null param bytea'ya bind olup patlamasın.
    @Query("""
            SELECT p FROM Product p
            WHERE p.status <> com.ecommerce.productservice.product.constant.ProductStatus.DELETED
              AND p.parentProduct IS NULL
              AND (:tenantId IS NULL OR p.tenantId = :tenantId)
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:status IS NULL OR p.status = :status)
              AND (:q IS NULL OR LOWER(p.name) LIKE :q OR LOWER(p.sku) LIKE :q)
            ORDER BY p.createdAt DESC
            """)
    Page<Product> searchForAdmin(@Param("q") String q,
                                 @Param("tenantId") Long tenantId,
                                 @Param("categoryId") Long categoryId,
                                 @Param("status") ProductStatus status,
                                 Pageable pageable);

    // Merchant ürün listesi araması (DELETED hariç, sadece ana ürünler).
    // :q önceden hazırlanmış LIKE deseni ("%...%", lowercase) ya da null (bytea bind tuzağı için CONCAT/LOWER param üstünde yok).
    @Query("""
            SELECT p FROM Product p
            WHERE p.tenantId = :tenantId
              AND p.parentProduct IS NULL
              AND p.status <> com.ecommerce.productservice.product.constant.ProductStatus.DELETED
              AND (:salesStatus IS NULL OR p.salesStatus = :salesStatus)
              AND (:q IS NULL OR LOWER(p.name) LIKE :q OR LOWER(p.sku) LIKE :q)
            """)
    Page<Product> searchForTenant(@Param("tenantId") Long tenantId,
                                  @Param("q") String q,
                                  @Param("salesStatus") SalesStatus salesStatus,
                                  Pageable pageable);

}