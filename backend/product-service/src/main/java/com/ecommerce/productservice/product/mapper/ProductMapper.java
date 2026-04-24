package com.ecommerce.productservice.product.mapper;

import com.ecommerce.productservice.product.command.ProductCreateContext;
import com.ecommerce.productservice.product.command.ProductUpdateContext;
import com.ecommerce.productservice.product.controller.dto.request.ProductUpdateRequest;
import com.ecommerce.productservice.product.entity.*;
import com.ecommerce.productservice.product.controller.dto.request.ProductCreateRequest;
import com.ecommerce.productservice.product.controller.dto.response.ProductResponse;
import com.ecommerce.productservice.product.query.ProductInfo;
import com.ecommerce.productservice.product.query.PublicProductInfo;
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

    ProductResponse toResponseFromInfo(ProductInfo info);

    ProductResponse toResponseFromPublicInfo(PublicProductInfo info);

    default ProductUpdateContext toUpdateContext(ProductUpdateRequest request, Long tenantId, UUID keycloakId) {
        return new ProductUpdateContext(
                tenantId,
                keycloakId,
                request.categoryId(),
                request.parentProductId(),
                request.name(),
                request.description(),
                request.sku(),
                request.brand(),
                request.price(),
                request.currency() != null ? request.currency() : "TRY",
                request.weightGrams(),
                request.dimensionsCm(),
                request.mainImageUrl(),
                request.imageUrls(),
                request.attributes(),
                request.minOrderQty(),
                request.maxOrderQty(),
                request.tags(),
                request.seoTitle(),
                request.seoDescription(),
                request.seoKeywords(),
                request.status(),
                request.salesStatus()
        );
    }

}