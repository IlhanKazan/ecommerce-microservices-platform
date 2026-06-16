package com.ecommerce.searchservice.product.mapper;

import com.ecommerce.contracts.event.product.ProductCreatedEventPayload;
import org.mapstruct.Mapper;
import com.ecommerce.searchservice.product.document.ProductDocument;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductSearchMapper {

    @Mapping(target = "id", expression = "java(payload.productId().toString())")
    @Mapping(target = "saleCount", constant = "0")
    @Mapping(target = "viewCount", constant = "0")
    @Mapping(target = "isFeatured", constant = "false")
    @Mapping(target = "ratingAverage", constant = "0.0")
    @Mapping(target = "reviewCount", constant = "0")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "salesStatus", expression = "java(\"ON_SALE\")")
    ProductDocument toDocument(ProductCreatedEventPayload payload);
}
