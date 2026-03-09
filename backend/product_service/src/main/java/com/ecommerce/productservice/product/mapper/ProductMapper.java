package com.ecommerce.productservice.product.mapper;

import com.ecommerce.productservice.product.entity.ProductCreateContext;
import com.ecommerce.productservice.product.controller.dto.request.ProductCreateRequest;
import com.ecommerce.productservice.product.controller.dto.response.ProductResponse;
import com.ecommerce.productservice.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductMapper {

    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "keycloakId", source = "userId")
    ProductCreateContext toContext(ProductCreateRequest request, Long tenantId, UUID userId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "parentProduct", ignore = true)
    @Mapping(target = "createdByUserId", source = "keycloakId")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "salesStatus", constant = "ON_SALE")
    Product toEntity(ProductCreateContext context);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "parentProduct.id", target = "parentProductId")
    ProductResponse toResponse(Product product);
}