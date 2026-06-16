package com.ecommerce.productservice.product.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Matris üretici: tek istekte birden çok varyant oluşturma. */
public record VariantBatchRequest(
        @NotEmpty(message = "Varyant listesi boş olamaz")
        @Valid
        List<VariantRequest> variants
) {
}
