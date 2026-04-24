package com.ecommerce.productservice.review.controller.dto.response;

import com.ecommerce.productservice.review.constant.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        Long id,
        UUID userId,
        String title,
        String reviewText,
        Integer rating,
        String sentimentLabel,
        Boolean isVerifiedPurchase,
        Integer helpfulCount,
        Integer notHelpfulCount,
        List<String> imageUrls,
        String sellerResponse,
        LocalDateTime sellerResponseAt,
        LocalDateTime reviewedAt,
        ReviewStatus status
) {}