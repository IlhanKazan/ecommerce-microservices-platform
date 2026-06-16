package com.ecommerce.usertenantservice.activity.controller.dto;

import jakarta.validation.constraints.NotNull;

/** Giriş yapmış kullanıcının ürün görüntüleme kaydı. */
public record RecordViewRequest(
        @NotNull Long productId,
        Long tenantId
) {
}
