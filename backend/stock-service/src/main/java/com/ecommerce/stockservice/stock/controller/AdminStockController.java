package com.ecommerce.stockservice.stock.controller;

import com.ecommerce.stockservice.stock.service.StockService;
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
public class AdminStockController {

    private final StockService stockService;

    // Dev aracı: ES'teki inStock flag'larını DB gerçeğiyle senkronize eder.
    // Seed data veya doğrudan DB insert sonrası çağrılır.
    // POST /api/v1/public/admin/stocks/resync
    @PostMapping("/stocks/resync")
    public ResponseEntity<String> resyncStockStatus() {
        int count = stockService.resyncStockStatus();
        return ResponseEntity.ok("Stok resync tamamlandı. " + count + " ürün için event yazıldı.");
    }
}
