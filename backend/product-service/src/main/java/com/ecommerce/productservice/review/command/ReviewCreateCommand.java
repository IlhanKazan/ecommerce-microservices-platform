package com.ecommerce.productservice.review.command;

import java.util.List;
import java.util.UUID;

public record ReviewCreateCommand(
        Long productId,
        UUID userId,
        String title,
        String reviewText,
        Integer rating,
        List<String> imageUrls
) {}