package com.ecommerce.productservice.outbox.service;

import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.review.entity.ProductReview;

public interface OutboxService {
    void publishProductCreatedEvent(Product product);
    void publishProductDeletedEvent(Product product);
    void publishProductUpdatedEvent(Product product);
    void publishReviewCreatedEvent(ProductReview review);
}
