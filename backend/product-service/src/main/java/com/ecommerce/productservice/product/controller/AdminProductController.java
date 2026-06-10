package com.ecommerce.productservice.product.controller;

import com.ecommerce.productservice.product.service.InternalProductService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/api/v1/public/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final InternalProductService internalProductService;

    // Dev aracı: ES'deki tüm ürünlere tenantName/tenantLogoUrl eklemek için
    // POST /api/v1/public/admin/reindex
    @PostMapping("/reindex")
    public ResponseEntity<String> reindex() {
        int count = internalProductService.reindexAllProducts();
        return ResponseEntity.ok("Reindex tamamlandı. " + count + " ürün için event yazıldı.");
    }
}
