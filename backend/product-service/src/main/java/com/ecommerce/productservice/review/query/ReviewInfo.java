package com.ecommerce.productservice.review.query;

import com.ecommerce.productservice.review.constant.ReviewStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReviewInfo(
        Long id,
        Long productId,
        UUID userId,
        String title,
        String reviewText,
        Integer rating,
        String sentimentLabel,
        Boolean isVerifiedPurchase,
        Integer helpfulCount,
        Integer notHelpfulCount,
        ReviewStatus status,
        List<String> imageUrls,
        String sellerResponse,
        LocalDateTime sellerResponseAt,
        LocalDateTime reviewedAt
) implements Serializable {}