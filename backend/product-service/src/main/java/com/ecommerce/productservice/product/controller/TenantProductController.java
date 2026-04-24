package com.ecommerce.productservice.product.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.productservice.common.constants.ApiPaths;
import com.ecommerce.productservice.product.controller.dto.request.ProductUpdateRequest;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.entity.ProductCreateContext;
import com.ecommerce.productservice.product.controller.dto.request.ProductCreateRequest;
import com.ecommerce.productservice.product.controller.dto.response.ProductResponse;
import com.ecommerce.productservice.product.entity.ProductInfo;
import com.ecommerce.productservice.product.entity.ProductUpdateContext;
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
            @PathVariable("tenantId") Long tenantId,
            @Valid @RequestBody ProductCreateRequest request,
            @CurrentUser AuthUser user) {

        ProductCreateContext context = productMapper.toContext(request, tenantId, user.keycloakId());
        Product savedProduct = tenantProductService.createProduct(context);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productMapper.toResponse(savedProduct));
    }

    @GetMapping("/{productId}")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<ProductResponse> getTenantProduct(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("productId") Long productId) {

        ProductInfo productInfo = tenantProductService.getProductInfoByIdAndTenantId(productId, tenantId);

        ProductResponse response = productMapper.toResponseFromInfo(productInfo);

        return ResponseEntity.ok(response);
    }

    @Idempotent(cachePrefix = "idempotency:product-update:")
    @PutMapping("/{productId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("productId") Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @CurrentUser AuthUser user) {

        ProductUpdateContext context = productMapper.toUpdateContext(request, tenantId, user.keycloakId());

        Product updatedProduct = tenantProductService.updateProduct(tenantId, productId, context);

        return ResponseEntity.ok(productMapper.toResponse(updatedProduct));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("productId") Long productId) {

        tenantProductService.deleteProduct(tenantId, productId);

        return ResponseEntity.noContent().build();
    }

}