package com.ecommerce.contracts.event.review;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewCreatedEventPayload(
        Long reviewId,
        Long productId,
        Long tenantId,
        UUID userId,
        String title,
        String reviewText,
        Integer rating,
        LocalDateTime reviewedAt
) {}
