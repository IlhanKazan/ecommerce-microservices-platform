package com.ecommerce.usertenantservice.activity.repository;

import com.ecommerce.usertenantservice.activity.constant.ActivityType;
import com.ecommerce.usertenantservice.activity.entity.UserActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    // Kullanıcının son gezdiği DISTINCT ürünler (en yeni görüntülenme önce) — "son gezilenler" + öneri.
    @Query("""
            SELECT a.productId FROM UserActivity a
            WHERE a.userId = :userId AND a.activityType = :type AND a.productId IS NOT NULL
            GROUP BY a.productId
            ORDER BY MAX(a.createdAt) DESC
            """)
    List<Long> findRecentlyViewedProductIds(@Param("userId") UUID userId,
                                            @Param("type") ActivityType type,
                                            Pageable pageable);

    // Son aramalar (distinct terim, en yeni önce).
    @Query("""
            SELECT a.searchTerm FROM UserActivity a
            WHERE a.userId = :userId AND a.activityType = :type AND a.searchTerm IS NOT NULL
            GROUP BY a.searchTerm
            ORDER BY MAX(a.createdAt) DESC
            """)
    List<String> findRecentSearchTerms(@Param("userId") UUID userId,
                                       @Param("type") ActivityType type,
                                       Pageable pageable);
}
