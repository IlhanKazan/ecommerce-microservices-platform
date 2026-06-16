package com.ecommerce.usertenantservice.activity.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Kullanıcının yaptığı arama terimi kaydı (son aramalar / AI sinyali). */
public record RecordSearchRequest(
        @NotBlank String term
) {
}
