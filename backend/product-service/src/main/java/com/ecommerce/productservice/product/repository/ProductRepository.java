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
    Page<Product> findAllByTenantIdAndStatusNot(Long tenantId, ProductStatus status, Pageable pageable);
    Page<Product> findByTenantIdAndCategoryIdAndStatusNot(
            Long tenantId, Long categoryId, ProductStatus status, Pageable pageable);
    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);
    List<Product> findAllByTenantId(Long tenantId);
    List<Product> findByCategoryIdAndStatus(Long categoryId, String status);
    List<Product> findByParentProductIdAndStatus(Long parentProductId, String status);

}