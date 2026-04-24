package com.ecommerce.productservice.review.repository;

import com.ecommerce.productservice.review.entity.ProductReview;
import com.ecommerce.productservice.review.constant.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<ProductReview, Long> {

    Page<ProductReview> findByProductIdAndStatus(
            Long productId, ReviewStatus status, Pageable pageable);

    // Bir kullanıcı aynı ürüne iki yorum yapamaz
    boolean existsByProductIdAndUserId(Long productId, UUID userId);

    Optional<ProductReview> findByIdAndUserId(Long id, UUID userId);

    // Rating güncellenince product tablosundaki aggregate'i güncelle
    @Modifying
    @Query("""
        UPDATE Product p SET
            p.ratingAverage = (
                SELECT COALESCE(AVG(CAST(r.rating AS double)), 0)
                FROM ProductReview r
                WHERE r.product.id = :productId
                AND r.status = 'APPROVED'
            ),
            p.reviewCount = (
                SELECT COUNT(r)
                FROM ProductReview r
                WHERE r.product.id = :productId
                AND r.status = 'APPROVED'
            )
        WHERE p.id = :productId
    """)
    void recalculateProductRating(@Param("productId") Long productId);
}