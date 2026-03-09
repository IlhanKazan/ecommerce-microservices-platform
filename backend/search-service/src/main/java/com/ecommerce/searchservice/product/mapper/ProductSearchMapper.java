package com.ecommerce.searchservice.product.mapper;

import org.mapstruct.Mapper;

import com.ecommerce.searchservice.product.document.ProductDocument;
import com.ecommerce.searchservice.product.controller.dto.ProductCreatedEventPayload;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductSearchMapper {

    // Kafka'dan gelen Long productId'yi, Elasticsearch'ün String ID'sine çeviriyoruz
    @Mapping(target = "id", expression = "java(payload.productId().toString())")
    ProductDocument toDocument(ProductCreatedEventPayload payload);
}
