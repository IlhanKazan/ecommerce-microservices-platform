package com.ecommerce.basketservice.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Guest sepetini hesap sepetine taşımak için. Yalnızca productId + quantity taşınır;
 * fiyat/isim/görsel backend'de product-service'ten taze çekilir (guest fiyatına güvenilmez).
 */
public record MergeBasketRequest(
        @NotNull
        @Valid
        List<AddItemRequest> items
) {}
