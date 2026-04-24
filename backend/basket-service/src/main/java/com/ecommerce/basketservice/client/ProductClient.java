package com.ecommerce.basketservice.client;

import com.ecommerce.basketservice.client.dto.ProductClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${application.clients.product.url}")
public interface ProductClient {

    @GetMapping("/api/v1/public/products/{id}")
    ProductClientResponse getProductById(@PathVariable("id") Long id);
}