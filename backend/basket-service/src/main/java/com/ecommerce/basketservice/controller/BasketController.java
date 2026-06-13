package com.ecommerce.basketservice.controller;

import com.ecommerce.basketservice.client.adapter.ProductClientAdapter;
import com.ecommerce.basketservice.controller.dto.AddItemRequest;
import com.ecommerce.basketservice.controller.dto.BasketItemResponse;
import com.ecommerce.basketservice.controller.dto.BasketResponse;
import com.ecommerce.basketservice.controller.dto.MergeBasketRequest;
import com.ecommerce.basketservice.controller.dto.UpdateItemQuantityRequest;
import com.ecommerce.basketservice.client.dto.ProductClientResponse;
import com.ecommerce.basketservice.entity.Basket;
import com.ecommerce.basketservice.entity.BasketItem;
import com.ecommerce.basketservice.service.BasketService;
import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.common.security.dto.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/baskets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Basket", description = "Shopping cart — add items, update quantities, clear cart. All operations require authentication.")
public class BasketController {

    private final BasketService basketService;
    private final ProductClientAdapter productClientAdapter;

    @Operation(summary = "Add item to basket", description = "Adds a product to the authenticated user's cart. If the product already exists in the cart, quantity is incremented. Validates product availability and stock via product-service.")
    @ApiResponse(responseCode = "200", description = "Item added — returns success message")
    @ApiResponse(responseCode = "404", description = "Product not found or not available for sale")
    @ApiResponse(responseCode = "409", description = "Requested quantity exceeds available stock")
    @Idempotent(cachePrefix = "idempotency:basket-add:", ttlSeconds = 300)
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

    @Operation(summary = "Get my basket", description = "Returns the full basket for the authenticated user including all items, quantities and current prices from product-service.")
    @ApiResponse(responseCode = "200", description = "Basket with items")
    @GetMapping("/me")
    public ResponseEntity<BasketResponse> getMyBasket(@CurrentUser AuthUser user) {
        Basket basket = basketService.getBasket(user.keycloakId());
        return ResponseEntity.ok(toBasketResponse(basket));
    }

    @Operation(summary = "Merge guest basket", description = "Giriş sonrası guest (local) sepetini hesap sepetiyle birleştirir. Aynı üründe miktarlar toplanır. Fiyat/isim product-service'ten taze çekilir; satışta olmayan ürünler atlanır.")
    @ApiResponse(responseCode = "200", description = "Birleştirilmiş sepet")
    @PostMapping("/me/merge")
    public ResponseEntity<BasketResponse> mergeBasket(
            @CurrentUser AuthUser user,
            @Valid @RequestBody MergeBasketRequest request) {

        List<BasketItem> enrichedItems = new ArrayList<>();
        for (AddItemRequest item : request.items()) {
            try {
                ProductClientResponse productDto =
                        productClientAdapter.validateAndGetProduct(item.productId(), item.quantity());
                enrichedItems.add(new BasketItem(
                        productDto.id(),
                        productDto.name(),
                        item.quantity(),
                        productDto.price(),
                        productDto.mainImageUrl()
                ));
            } catch (Exception e) {
                // Guest sepetinde artık satışta olmayan/yetersiz stoklu ürün olabilir → atla, merge'i bozma
                log.warn("Merge sırasında ürün eklenemedi, atlanıyor. productId: {}, hata: {}",
                        item.productId(), e.getMessage());
            }
        }

        Basket basket = basketService.mergeItems(user.keycloakId(), enrichedItems);
        return ResponseEntity.ok(toBasketResponse(basket));
    }

    @Operation(summary = "Remove item from basket", description = "Removes a specific product from the cart entirely regardless of quantity.")
    @ApiResponse(responseCode = "204", description = "Item removed")
    @ApiResponse(responseCode = "404", description = "Product not in basket")
    @DeleteMapping("/me/items/{productId}")
    public ResponseEntity<Void> removeItemFromBasket(
            @CurrentUser AuthUser user,
            @PathVariable Long productId) {
        basketService.removeItemFromBasket(user.keycloakId(), productId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update item quantity", description = "Sets the quantity for a cart item. Use quantity=0 to remove the item, or DELETE endpoint instead.")
    @ApiResponse(responseCode = "204", description = "Quantity updated")
    @PatchMapping("/me/items/{productId}")
    public ResponseEntity<Void> updateItemQuantity(
            @CurrentUser AuthUser user,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateItemQuantityRequest request) {
        basketService.setItemQuantity(user.keycloakId(), productId, request.quantity());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Clear basket", description = "Removes all items from the authenticated user's cart.")
    @ApiResponse(responseCode = "204", description = "Basket cleared")
    @DeleteMapping("/me")
    public ResponseEntity<Void> clearMyBasket(@CurrentUser AuthUser user) {
        basketService.deleteBasket(user.keycloakId());
        return ResponseEntity.noContent().build();
    }

    private BasketResponse toBasketResponse(Basket basket) {
        List<BasketItemResponse> itemResponses = basket.getItems().stream()
                .map(item -> new BasketItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getMainImageUrl()
                )).toList();

        return new BasketResponse(
                basket.getUserId(),
                itemResponses,
                basket.calculateTotalPrice()
        );
    }
}