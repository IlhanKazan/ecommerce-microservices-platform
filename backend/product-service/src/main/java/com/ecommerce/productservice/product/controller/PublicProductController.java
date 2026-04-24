package com.ecommerce.productservice.product.controller;

import com.ecommerce.productservice.common.constants.ApiPaths;
import com.ecommerce.productservice.product.controller.dto.response.ProductResponse;
import com.ecommerce.productservice.product.mapper.ProductMapper;
import com.ecommerce.productservice.product.query.PublicProductInfo;
import com.ecommerce.productservice.product.service.QueryProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.PublicProduct.PUBLIC_PRODUCTS)
@RequiredArgsConstructor
public class PublicProductController {

    private final QueryProductService queryProductService;
    private final ProductMapper productMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        PublicProductInfo product = queryProductService.getPublicProductInfo(id);
        ProductResponse response = productMapper.toResponseFromPublicInfo(product);
        return ResponseEntity.ok(response);
    }
}