package com.ecommerce.productservice.product.controller;

import com.ecommerce.productservice.common.constants.ApiPaths;
import com.ecommerce.productservice.product.controller.dto.response.ProductResponse;
import com.ecommerce.productservice.product.mapper.ProductMapper;
import com.ecommerce.productservice.product.query.PublicProductInfo;
import com.ecommerce.productservice.product.service.QueryProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Public Products", description = "Public product detail for customer-facing pages — no authentication required")
@RestController
@RequestMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS)
@RequiredArgsConstructor
public class PublicProductController {

    private final QueryProductService queryProductService;
    private final ProductMapper productMapper;

    @Operation(summary = "Get public product", description = "Returns product detail visible to customers: name, price, images, description, reviews summary.", security = {})
    @ApiResponse(responseCode = "200", description = "Product detail")
    @ApiResponse(responseCode = "404", description = "Product not found or deleted")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        PublicProductInfo product = queryProductService.getPublicProductInfo(id);
        ProductResponse response = productMapper.toResponseFromPublicInfo(product);
        return ResponseEntity.ok(response);
    }
}