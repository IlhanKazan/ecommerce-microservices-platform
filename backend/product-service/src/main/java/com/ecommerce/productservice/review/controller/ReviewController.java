package com.ecommerce.productservice.review.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.productservice.common.constants.ApiPaths;
import com.ecommerce.productservice.common.service.ImageService;
import com.ecommerce.productservice.review.command.ReviewCreateCommand;
import com.ecommerce.productservice.review.controller.dto.request.ReviewCreateRequest;
import com.ecommerce.productservice.review.controller.dto.request.SellerResponseRequest;
import com.ecommerce.productservice.review.controller.dto.response.ReviewResponse;
import com.ecommerce.productservice.review.mapper.ReviewMapper;
import com.ecommerce.productservice.review.query.ReviewInfo;
import com.ecommerce.productservice.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "Reviews", description = "Product reviews — submit, list, mark helpful, seller responses")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    private final ImageService imageService;

    @Operation(summary = "List product reviews", description = "Returns paginated reviews for a product, ordered by most recent.", security = {})
    @ApiResponse(responseCode = "200", description = "Review list")
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

    @Operation(summary = "Create product review", description = "Submits a rating and text review for a product. User must have purchased the product (future enforcement).")
    @ApiResponse(responseCode = "200", description = "Review created")
    @ApiResponse(responseCode = "409", description = "User already reviewed this product")
    // POST /api/v1/public/products/{productId}/reviews — auth gerekli
    @Idempotent(cachePrefix = "idempotency:review-create:", ttlSeconds = 300)
    @PostMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS + "/{productId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewCreateRequest request,
            @CurrentUser AuthUser user) {

        String displayName = (user.firstName() != null && !user.firstName().isBlank())
                ? (user.firstName() + (user.lastName() != null ? " " + user.lastName() : "")).trim()
                : user.username();

        ReviewCreateCommand command = new ReviewCreateCommand(
                productId,
                user.keycloakId(),
                displayName,
                request.title(),
                request.reviewText(),
                request.rating(),
                request.imageUrls()
        );

        ReviewInfo info = reviewService.createReview(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewMapper.toResponse(info));
    }

    @Operation(summary = "Mark review helpful/unhelpful", description = "Authenticated users can vote once per review. Duplicate votes return 409.")
    @ApiResponse(responseCode = "200", description = "Vote recorded")
    @ApiResponse(responseCode = "409", description = "Already voted on this review")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS
            + "/{productId}/reviews/{reviewId}/helpful")
    public ResponseEntity<Void> markHelpful(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @RequestParam boolean helpful,
            @CurrentUser AuthUser user) {

        reviewService.markHelpful(reviewId, helpful, user.keycloakId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Add seller response", description = "Merchant owner adds a public reply to a customer review.")
    @ApiResponse(responseCode = "200", description = "Response added")
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

    @Operation(summary = "Upload review image", description = "Uploads a single image for a review. Auth required; no seller ownership check.")
    @ApiResponse(responseCode = "200", description = "Uploaded image URL")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS + "/{productId}/reviews/images/upload")
    public ResponseEntity<Map<String, String>> uploadReviewImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        String url = imageService.uploadImage(file, "review-images/" + productId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @Operation(summary = "Delete review", description = "Soft-deletes a review. Only the review author can delete their own review.")
    @ApiResponse(responseCode = "200", description = "Review deleted")
    @ApiResponse(responseCode = "403", description = "Not the review author")
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