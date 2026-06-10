package com.ecommerce.productservice.review.repository;

import com.ecommerce.productservice.review.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {
    boolean existsByReviewIdAndUserId(Long reviewId, UUID userId);
}
