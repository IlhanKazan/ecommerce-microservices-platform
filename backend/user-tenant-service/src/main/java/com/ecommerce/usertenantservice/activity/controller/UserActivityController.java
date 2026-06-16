package com.ecommerce.usertenantservice.activity.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.usertenantservice.activity.controller.dto.RecordSearchRequest;
import com.ecommerce.usertenantservice.activity.controller.dto.RecordViewRequest;
import com.ecommerce.usertenantservice.activity.service.UserActivityService;
import com.ecommerce.usertenantservice.common.constants.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.User.ACTIVITY)
@RequiredArgsConstructor
@Tag(name = "Kullanıcı Aktivitesi", description = "Gezinme geçmişi — kişiselleştirilmiş öneri + AI beslemesi için")
public class UserActivityController {

    private final UserActivityService userActivityService;

    @Operation(summary = "Record product view", description = "Giriş yapmış kullanıcının ürün görüntülemesini kaydeder (best-effort).")
    @PostMapping("/views")
    public ResponseEntity<Void> recordView(
            @CurrentUser AuthUser user,
            @Valid @RequestBody RecordViewRequest request) {
        userActivityService.recordProductView(user.keycloakId(), request.productId(), request.tenantId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Recently viewed products", description = "Kullanıcının son gezdiği ürün id'leri (en yeni önce). Öneri/'son gezilenler' için.")
    @GetMapping("/recently-viewed")
    public ResponseEntity<List<Long>> recentlyViewed(
            @CurrentUser AuthUser user,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(userActivityService.getRecentlyViewedProductIds(user.keycloakId(), limit));
    }

    @Operation(summary = "Record search", description = "Kullanıcının arama terimini kaydeder (son aramalar + AI sinyali).")
    @PostMapping("/searches")
    public ResponseEntity<Void> recordSearch(
            @CurrentUser AuthUser user,
            @Valid @RequestBody RecordSearchRequest request) {
        userActivityService.recordSearch(user.keycloakId(), request.term());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Recent searches", description = "Kullanıcının son arama terimleri (distinct, en yeni önce).")
    @GetMapping("/recent-searches")
    public ResponseEntity<List<String>> recentSearches(
            @CurrentUser AuthUser user,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(userActivityService.getRecentSearches(user.keycloakId(), limit));
    }
}
