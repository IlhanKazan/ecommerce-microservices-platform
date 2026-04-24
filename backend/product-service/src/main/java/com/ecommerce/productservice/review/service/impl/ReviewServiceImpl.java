package com.ecommerce.productservice.review.service.impl;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.repository.ProductRepository;
import com.ecommerce.productservice.review.command.ReviewCreateCommand;
import com.ecommerce.productservice.review.constant.ReviewStatus;
import com.ecommerce.productservice.review.entity.ProductReview;
import com.ecommerce.productservice.review.query.ReviewInfo;
import com.ecommerce.productservice.review.repository.ReviewRepository;
import com.ecommerce.productservice.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ReviewInfo createReview(ReviewCreateCommand command) {
        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ürün bulunamadı.", "PRODUCT_NOT_FOUND"));

        if (reviewRepository.existsByProductIdAndUserId(
                command.productId(), command.userId())) {
            throw new BusinessException(
                    "Bu ürün için zaten bir yorumunuz var.", "REVIEW_ALREADY_EXISTS");
        }

        ProductReview review = ProductReview.builder()
                .product(product)
                .userId(command.userId())
                .title(command.title())
                .reviewText(command.reviewText())
                .rating(command.rating())
                .imageUrls(command.imageUrls())
                .isVerifiedPurchase(false)
                .status(ReviewStatus.PENDING)
                .reviewedAt(LocalDateTime.now())
                .build();

        ProductReview saved = reviewRepository.save(review);
        log.info("Yorum oluşturuldu. Product: {}, User: {}, Status: PENDING",
                command.productId(), command.userId());

        return toInfo(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewInfo> getApprovedReviews(Long productId, int page, int size) {
        PageRequest pageable = PageRequest.of(
                page, size, Sort.by("reviewedAt").descending());

        Page<ReviewInfo> result = reviewRepository
                .findByProductIdAndStatus(productId, ReviewStatus.APPROVED, pageable)
                .map(this::toInfo);

        return PageResponse.of(result);
    }

    @Override
    @Transactional
    public void markHelpful(Long reviewId, boolean helpful) {
        ProductReview review = findReviewById(reviewId);

        if (helpful) {
            review.setHelpfulCount(review.getHelpfulCount() + 1);
        } else {
            review.setNotHelpfulCount(review.getNotHelpfulCount() + 1);
        }

        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void addSellerResponse(Long reviewId, Long tenantId,
                                  String response, UUID keycloakId) {
        ProductReview review = findReviewById(reviewId);

        // Yorum bu mağazanın ürününe mi ait?
        if (!review.getProduct().getTenantId().equals(tenantId)) {
            throw new BusinessException(
                    "Bu yorum bu mağazaya ait değil.", "REVIEW_NOT_OWNED");
        }

        if (review.getSellerResponse() != null) {
            throw new BusinessException(
                    "Bu yoruma zaten yanıt verilmiş.", "SELLER_RESPONSE_EXISTS");
        }

        review.setSellerResponse(response);
        review.setSellerResponseAt(LocalDateTime.now());
        reviewRepository.save(review);

        log.info("Satıcı yanıtı eklendi. ReviewId: {}, Tenant: {}", reviewId, tenantId);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, UUID userId) {
        ProductReview review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Yorum bulunamadı veya size ait değil.", "REVIEW_NOT_FOUND"));

        reviewRepository.delete(review);

        // Rating aggregate'ini güncelle
        reviewRepository.recalculateProductRating(review.getProduct().getId());
        log.info("Yorum silindi ve rating güncellendi. ReviewId: {}", reviewId);
    }

    private ProductReview findReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Yorum bulunamadı.", "REVIEW_NOT_FOUND"));
    }

    private ReviewInfo toInfo(ProductReview r) {
        return new ReviewInfo(
                r.getId(),
                r.getProduct().getId(),
                r.getUserId(),
                r.getTitle(),
                r.getReviewText(),
                r.getRating(),
                r.getSentimentLabel(),
                r.getIsVerifiedPurchase(),
                r.getHelpfulCount(),
                r.getNotHelpfulCount(),
                r.getStatus(),
                r.getImageUrls(),
                r.getSellerResponse(),
                r.getSellerResponseAt(),
                r.getReviewedAt()
        );
    }
}