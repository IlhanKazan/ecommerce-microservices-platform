package com.ecommerce.productservice.review.mapper;

import com.ecommerce.productservice.review.controller.dto.response.ReviewResponse;
import com.ecommerce.productservice.review.query.ReviewInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    ReviewResponse toResponse(ReviewInfo info);
}