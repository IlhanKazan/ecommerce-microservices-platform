package com.ecommerce.usertenantservice.tenant.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantVerificationRequest(

        @NotBlank(message = "IBAN alanı boş bırakılamaz.")
        @Pattern(regexp = "^TR\\d{24}$", message = "Lütfen 'TR' ile başlayan 26 haneli geçerli bir IBAN giriniz.")
        String iban,

        @NotBlank(message = "Vergi dairesi zorunludur.")
        String taxOffice,

        @NotBlank(message = "Firma ünvanı boş bırakılamaz.")
        @Size(min = 3, max = 100, message = "Firma ünvanı 3 ile 100 karakter arasında olmalıdır.")
        String legalCompanyTitle

) {}
