package com.ecommerce.productservice.review.controller.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

public record ReviewCreateRequest(
        @NotBlank(message = "Başlık boş olamaz")
        String title,

        @NotBlank(message = "Yorum metni boş olamaz")
        String reviewText,

        @NotNull @Min(1) @Max(5)
        Integer rating,

        List<String> imageUrls
) {}