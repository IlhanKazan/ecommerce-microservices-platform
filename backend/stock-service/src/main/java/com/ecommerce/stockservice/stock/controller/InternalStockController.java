package com.ecommerce.stockservice.stock.controller;

import com.ecommerce.stockservice.common.constants.ApiPaths;
import com.ecommerce.stockservice.stock.controller.dto.request.InternalStockReserveRequest;
import com.ecommerce.stockservice.stock.service.InternalStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.Internal.INTERNAL_STOCKS)
@RequiredArgsConstructor
@Slf4j
public class InternalStockController {

    private final InternalStockService internalStockService;

    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(@RequestBody InternalStockReserveRequest request) {
        internalStockService.reserveAllForOrder(
                request.orderId(), request.tenantId(), request.items());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rollback")
    public ResponseEntity<Void> rollback(@RequestBody InternalStockReserveRequest request) {
        internalStockService.rollbackAllForOrder(
                request.orderId(), request.tenantId(), request.items());
        return ResponseEntity.ok().build();
    }
}
