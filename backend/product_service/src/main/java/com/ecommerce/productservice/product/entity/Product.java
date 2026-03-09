package com.ecommerce.productservice.product.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.productservice.category.entity.Category;
import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_product_id")
    private Product parentProduct;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(length = 100)
    private String brand;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 3)
    @Builder.Default
    private String currency = "TRY";

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountedPrice;

    private Integer weightGrams;

    @Column(length = 50)
    private String dimensionsCm;

    @Column(length = 500)
    private String mainImageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> imageUrls;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> attributes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SalesStatus salesStatus = SalesStatus.ON_SALE;

    @Builder.Default
    private Boolean isFeatured = false;

    @Builder.Default
    private Integer viewCount = 0;

    @Builder.Default
    private Integer saleCount = 0;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Builder.Default
    private Integer reviewCount = 0;

    @Builder.Default
    private Integer minOrderQty = 1;
    private Integer maxOrderQty;

    @Column(columnDefinition = "TEXT")
    private String aiReviewReport;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> tags;

    @Column(columnDefinition = "TEXT")
    private String seoTitle;
    @Column(columnDefinition = "TEXT")
    private String seoDescription;
    @Column(columnDefinition = "TEXT")
    private String seoKeywords;

    private UUID createdByUserId;

}