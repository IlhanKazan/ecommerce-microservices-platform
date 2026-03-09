package com.ecommerce.productservice.review.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.review.constant.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private UUID userId;

    @Column(columnDefinition = "TEXT")
    private String reviewText;

    private Integer rating;

    private String title;

    @Column(length = 20)
    private String sentimentLabel; // AI Sentiment

    private Float sentimentScore;

    @Builder.Default
    private Boolean isVerifiedPurchase = false;

    @Builder.Default
    private Integer helpfulCount = 0;

    @Builder.Default
    private Integer notHelpfulCount = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    private LocalDateTime reviewedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> imageUrls;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> keywords;

    @Column(columnDefinition = "TEXT")
    private String sellerResponse;

    private LocalDateTime sellerResponseAt;

}