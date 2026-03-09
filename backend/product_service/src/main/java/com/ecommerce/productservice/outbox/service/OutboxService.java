package com.ecommerce.productservice.outbox.service;

import com.ecommerce.productservice.product.entity.Product;

public interface OutboxService {
    void publishProductCreatedEvent(Product product);
}
