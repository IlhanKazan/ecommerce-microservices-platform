package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.client.dto.StockReserveRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "stock-service", url = "${application.clients.stock.url}")
public interface StockServiceClient {

    @PostMapping("/api/v1/stocks/internal/reserve")
    void reserveStock(@RequestBody StockReserveRequest request);

    @PostMapping("/api/v1/stocks/internal/rollback")
    void rollbackStock(@RequestBody StockReserveRequest request);
}
