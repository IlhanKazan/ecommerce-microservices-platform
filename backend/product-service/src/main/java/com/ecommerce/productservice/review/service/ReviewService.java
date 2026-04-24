package com.ecommerce.productservice.review.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.productservice.review.command.ReviewCreateCommand;
import com.ecommerce.productservice.review.query.ReviewInfo;

import java.util.UUID;

public interface ReviewService {
    ReviewInfo createReview(ReviewCreateCommand command);

    PageResponse<ReviewInfo> getApprovedReviews(Long productId, int page, int size);

    void markHelpful(Long reviewId, boolean helpful);

    void addSellerResponse(Long reviewId, Long tenantId,
                           String response, UUID keycloakId);

    void deleteReview(Long reviewId, UUID userId);
}