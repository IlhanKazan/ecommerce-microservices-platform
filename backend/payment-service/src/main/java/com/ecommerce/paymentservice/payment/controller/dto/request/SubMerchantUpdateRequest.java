package com.ecommerce.paymentservice.payment.controller.dto.request;

public record SubMerchantUpdateRequest(
        Long tenantId,
        String businessType,
        String iban,
        String taxOffice,
        String taxId,
        String legalCompanyTitle,
        String email,
        String phone,
        String contactName,
        String contactSurname,
        String address,
        String existingSubMerchantKey
) {}
