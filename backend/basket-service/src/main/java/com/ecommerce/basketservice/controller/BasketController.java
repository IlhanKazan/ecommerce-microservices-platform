package com.ecommerce.basketservice.controller;

import com.ecommerce.basketservice.client.adapter.ProductClientAdapter;
import com.ecommerce.basketservice.controller.dto.AddItemRequest;
import com.ecommerce.basketservice.controller.dto.BasketItemResponse;
import com.ecommerce.basketservice.controller.dto.BasketResponse;
import com.ecommerce.basketservice.client.dto.ProductClientResponse;
import com.ecommerce.basketservice.entity.Basket;
import com.ecommerce.basketservice.entity.BasketItem;
import com.ecommerce.basketservice.service.BasketService;
import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.security.dto.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/baskets")
@RequiredArgsConstructor
public class BasketController {

    private final BasketService basketService;
    private final ProductClientAdapter productClientAdapter;

    @PostMapping("/me/items")
    public ResponseEntity<String> addItemToBasket(
            @CurrentUser AuthUser user,
            @Valid @RequestBody AddItemRequest request) {

        ProductClientResponse productDto = productClientAdapter.validateAndGetProduct(request.productId(), request.quantity());

        BasketItem newItem = new BasketItem(
                productDto.id(),
                productDto.name(),
                request.quantity(),
                productDto.price(),
                productDto.mainImageUrl()
        );

        basketService.addItemToBasket(user.keycloakId(), newItem);

        return ResponseEntity.ok("Ürün sepete başarıyla eklendi.");
    }

    @GetMapping("/me")
    public ResponseEntity<BasketResponse> getMyBasket(@CurrentUser AuthUser user) {
        Basket basket = basketService.getBasket(user.keycloakId());

        List<BasketItemResponse> itemResponses = basket.getItems().stream()
                .map(item -> new BasketItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getImageUrl()
                )).toList();

        BasketResponse response = new BasketResponse(
                basket.getUserId(),
                itemResponses,
                basket.calculateTotalPrice()
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> clearMyBasket(@CurrentUser AuthUser user) {
        basketService.deleteBasket(user.keycloakId());
        return ResponseEntity.noContent().build();
    }
}