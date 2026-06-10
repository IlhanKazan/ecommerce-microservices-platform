package com.ecommerce.orderservice.order.controller;

import com.ecommerce.orderservice.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderRepository orderRepository;

    @GetMapping("/has-purchased")
    public ResponseEntity<Boolean> hasPurchased(
            @RequestParam UUID userId,
            @RequestParam Long productId) {
        return ResponseEntity.ok(orderRepository.hasPurchased(userId, productId));
    }
}
