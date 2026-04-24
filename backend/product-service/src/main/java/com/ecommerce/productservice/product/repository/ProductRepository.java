package com.ecommerce.productservice.product.repository;

import com.ecommerce.productservice.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndTenantId(Long id, Long tenantId);
    List<Product> findAllByTenantId(Long tenantId);
    List<Product> findByCategoryIdAndStatus(Long categoryId, String status);
    List<Product> findByParentProductIdAndStatus(Long parentProductId, String status);
}