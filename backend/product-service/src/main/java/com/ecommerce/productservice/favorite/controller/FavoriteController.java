package com.ecommerce.productservice.favorite.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.productservice.common.constants.ApiPaths;
import com.ecommerce.productservice.favorite.service.FavoriteService;
import com.ecommerce.productservice.product.controller.dto.response.ProductResponse;
import com.ecommerce.productservice.product.mapper.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@Tag(name = "Favorites", description = "Kullanıcının favori ürünleri (wishlist) — giriş gerektirir")
@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final ProductMapper productMapper;

    @Operation(summary = "Add product to favorites")
    @ApiResponse(responseCode = "200", description = "Favoriye eklendi (idempotent)")
    @PreAuthorize("isAuthenticated()")
    @PutMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS + "/{productId}/favorite")
    public ResponseEntity<Void> addFavorite(@PathVariable Long productId, @CurrentUser AuthUser user) {
        favoriteService.addFavorite(user.keycloakId(), productId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove product from favorites")
    @ApiResponse(responseCode = "204", description = "Favoriden çıkarıldı")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS + "/{productId}/favorite")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long productId, @CurrentUser AuthUser user) {
        favoriteService.removeFavorite(user.keycloakId(), productId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Favorite product ids", description = "Kalp state'i için: kullanıcının favori ürün id'leri.")
    @ApiResponse(responseCode = "200", description = "Favori id listesi")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS + "/me/favorite-ids")
    public ResponseEntity<Set<Long>> getFavoriteIds(@CurrentUser AuthUser user) {
        return ResponseEntity.ok(favoriteService.getFavoriteProductIds(user.keycloakId()));
    }

    @Operation(summary = "List favorite products", description = "Favorilerim sayfası için favori ürünlerin detayları.")
    @ApiResponse(responseCode = "200", description = "Favori ürün listesi")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS + "/me/favorites")
    public ResponseEntity<List<ProductResponse>> getFavorites(@CurrentUser AuthUser user) {
        List<ProductResponse> response = favoriteService.getFavoriteProducts(user.keycloakId())
                .stream()
                .map(productMapper::toResponseFromPublicInfo)
                .toList();
        return ResponseEntity.ok(response);
    }
}
