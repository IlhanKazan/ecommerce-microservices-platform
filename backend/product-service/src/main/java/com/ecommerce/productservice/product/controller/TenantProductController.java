package com.ecommerce.productservice.product.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.productservice.common.constants.ApiPaths;
import com.ecommerce.productservice.product.constant.SalesStatus;
import com.ecommerce.productservice.product.controller.dto.request.ProductUpdateRequest;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.command.ProductCreateContext;
import com.ecommerce.productservice.product.controller.dto.request.ProductCreateRequest;
import com.ecommerce.productservice.product.controller.dto.response.ProductResponse;
import com.ecommerce.productservice.product.query.ProductInfo;
import com.ecommerce.productservice.product.command.ProductUpdateContext;
import com.ecommerce.productservice.product.mapper.ProductMapper;
import com.ecommerce.productservice.product.service.TenantProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.TenantProduct.TENANT_PRODUCTS)
@RequiredArgsConstructor
public class TenantProductController {

    private final TenantProductService tenantProductService;
    private final ProductMapper productMapper;

    @PostMapping
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<ProductResponse> createProduct(
            @PathVariable Long tenantId,
            @Valid @RequestBody ProductCreateRequest request,
            @CurrentUser AuthUser user) {

        ProductCreateContext context = productMapper.toContext(request, tenantId, user.keycloakId());
        Product saved = tenantProductService.createProduct(context);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productMapper.toResponse(saved));
    }

    // GET /api/v1/products/tenants/{tenantId}?page=0&size=20
    @GetMapping
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<PageResponse<ProductResponse>> getTenantProducts(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<ProductInfo> result =
                tenantProductService.getTenantProducts(tenantId, page, size);

        PageResponse<ProductResponse> response = new PageResponse<>(
                result.content().stream()
                        .map(productMapper::toResponseFromInfo)
                        .toList(),
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.isLast()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<ProductResponse> getTenantProduct(
            @PathVariable Long tenantId,
            @PathVariable Long productId) {

        ProductInfo info = tenantProductService
                .getProductInfoByIdAndTenantId(productId, tenantId);
        return ResponseEntity.ok(productMapper.toResponseFromInfo(info));
    }

    @Idempotent(cachePrefix = "idempotency:product-update:")
    @PutMapping("/{productId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long tenantId,
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @CurrentUser AuthUser user) {

        ProductUpdateContext context =
                productMapper.toUpdateContext(request, tenantId, user.keycloakId());
        Product updated = tenantProductService.updateProduct(tenantId, productId, context);
        return ResponseEntity.ok(productMapper.toResponse(updated));
    }

    // PATCH — sadece satış durumunu değiştir, tüm ürünü yollama
    @PatchMapping("/{productId}/sales-status")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> changeSalesStatus(
            @PathVariable Long tenantId,
            @PathVariable Long productId,
            @RequestParam SalesStatus status) {

        tenantProductService.changeSalesStatus(tenantId, productId, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long tenantId,
            @PathVariable Long productId) {

        tenantProductService.deleteProduct(tenantId, productId);
        return ResponseEntity.noContent().build();
    }
}