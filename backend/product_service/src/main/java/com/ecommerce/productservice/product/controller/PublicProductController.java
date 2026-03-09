package com.ecommerce.productservice.product.controller;

import com.ecommerce.productservice.common.constants.ApiPaths;
// import com.ecommerce.productservice.product.dto.response.ProductResponse;
// import com.ecommerce.productservice.product.service.ProductQueryService; // İleride okuma işlemlerini de ayırabiliriz (CQRS)
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS)
@RequiredArgsConstructor
public class PublicProductController {

    @GetMapping
    public ResponseEntity<String> getAllProducts() {
        // TODO [09.03.2026 11:49]: buraya ElasticSearch veya Redis'ten ürünleri çeken pagination'lı metot gelecek.
        return ResponseEntity.ok("Müşteriler için ürün listesi yakında burada olacak!");
    }
}
