package com.ecommerce.productservice.review.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ReviewSentimentUpdateRequest(
        @NotBlank(message = "Sentiment label boş olamaz")
        String sentimentLabel,
        Float sentimentScore,
        List<String> keywords
) {}
