package com.ecommerce.productservice.review.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.productservice.common.constants.ApiPaths;
import com.ecommerce.productservice.review.command.ReviewCreateCommand;
import com.ecommerce.productservice.review.controller.dto.request.ReviewCreateRequest;
import com.ecommerce.productservice.review.controller.dto.request.SellerResponseRequest;
import com.ecommerce.productservice.review.controller.dto.response.ReviewResponse;
import com.ecommerce.productservice.review.mapper.ReviewMapper;
import com.ecommerce.productservice.review.query.ReviewInfo;
import com.ecommerce.productservice.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;

    // GET /api/v1/public/products/{productId}/reviews
    @GetMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS + "/{productId}/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ReviewInfo> result =
                reviewService.getApprovedReviews(productId, page, size);

        PageResponse<ReviewResponse> response = new PageResponse<>(
                result.content().stream().map(reviewMapper::toResponse).toList(),
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.isLast()
        );

        return ResponseEntity.ok(response);
    }

    // POST /api/v1/public/products/{productId}/reviews — auth gerekli
    @PostMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS + "/{productId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewCreateRequest request,
            @CurrentUser AuthUser user) {

        ReviewCreateCommand command = new ReviewCreateCommand(
                productId,
                user.keycloakId(),
                request.title(),
                request.reviewText(),
                request.rating(),
                request.imageUrls()
        );

        ReviewInfo info = reviewService.createReview(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewMapper.toResponse(info));
    }

    // PATCH — helpful/not helpful oyu
    @PatchMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS
            + "/{productId}/reviews/{reviewId}/helpful")
    public ResponseEntity<Void> markHelpful(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @RequestParam boolean helpful) {

        reviewService.markHelpful(reviewId, helpful);
        return ResponseEntity.ok().build();
    }

    // POST — satıcı yanıtı, OWNER'a özel
    @PostMapping(ApiPaths.TenantProduct.TENANT_PRODUCTS
            + "/{productId}/reviews/{reviewId}/response")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> addSellerResponse(
            @PathVariable Long tenantId,
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @Valid @RequestBody SellerResponseRequest request,
            @CurrentUser AuthUser user) {

        reviewService.addSellerResponse(
                reviewId, tenantId, request.response(), user.keycloakId());
        return ResponseEntity.ok().build();
    }

    // DELETE — yorumu yazan silebilir
    @DeleteMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS
            + "/{productId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @CurrentUser AuthUser user) {

        reviewService.deleteReview(reviewId, user.keycloakId());
        return ResponseEntity.noContent().build();
    }
}