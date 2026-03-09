package com.ecommerce.productservice.product.service;

import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.entity.ProductCreateContext;

public interface ProductService {
    Product createProduct(ProductCreateContext context);
}
