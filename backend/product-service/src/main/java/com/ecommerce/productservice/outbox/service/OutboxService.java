package com.ecommerce.productservice.outbox.service;

import com.ecommerce.productservice.product.entity.Product;

public interface OutboxService {
    void publishProductCreatedEvent(Product product);
    void publishProductDeletedEvent(Product product);
    void publishProductUpdatedEvent(Product product);
}
