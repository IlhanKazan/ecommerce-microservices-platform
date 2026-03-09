package com.ecommerce.productservice.product.controller;

import com.ecommerce.common.security.annotation.CurrentUser;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.productservice.common.constants.ApiPaths;
import com.ecommerce.productservice.product.entity.ProductCreateContext;
import com.ecommerce.productservice.product.controller.dto.request.ProductCreateRequest;
import com.ecommerce.productservice.product.controller.dto.response.ProductResponse;
import com.ecommerce.productservice.product.mapper.ProductMapper;
import com.ecommerce.productservice.product.service.ProductService;
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

    private final ProductService productService;
    private final ProductMapper productMapper;

    @PostMapping
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<ProductResponse> createProduct(
            @PathVariable("tenantId") Long tenantId,
            @Valid @RequestBody ProductCreateRequest request,
            @CurrentUser AuthUser user) {

        ProductCreateContext context = productMapper.toContext(request, tenantId, user.keycloakId());
        var savedProduct = productService.createProduct(context);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productMapper.toResponse(savedProduct));
    }
}