package com.ecommerce.productservice.review.controller;

import com.ecommerce.productservice.review.controller.dto.request.ReviewSentimentUpdateRequest;
import com.ecommerce.productservice.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/products")
@RequiredArgsConstructor
public class InternalReviewController {

    private final ReviewService reviewService;

    @PatchMapping("/reviews/{reviewId}/sentiment")
    public ResponseEntity<Void> updateSentiment(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewSentimentUpdateRequest request) {
        reviewService.updateSentiment(reviewId, request.sentimentLabel(),
                request.sentimentScore(), request.keywords());
        return ResponseEntity.ok().build();
    }
}
