package com.ecommerce.productservice.favorite.repository;

import com.ecommerce.productservice.favorite.entity.ProductFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ProductFavoriteRepository extends JpaRepository<ProductFavorite, Long> {

    boolean existsByUserIdAndProductId(UUID userId, Long productId);

    void deleteByUserIdAndProductId(UUID userId, Long productId);

    List<ProductFavorite> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("select f.productId from ProductFavorite f where f.userId = :userId")
    Set<Long> findProductIdsByUserId(UUID userId);
}
