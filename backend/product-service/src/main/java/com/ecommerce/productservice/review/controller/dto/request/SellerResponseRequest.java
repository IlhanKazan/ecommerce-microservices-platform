package com.ecommerce.productservice.review.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SellerResponseRequest(
        @NotBlank(message = "Yanıt boş olamaz")
        String response
) {}