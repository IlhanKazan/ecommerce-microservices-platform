package com.ecommerce.contracts.event.tenant;

/**
 * Mağaza yaşam döngüsü durum değişikliği (PASSIVE/CLOSED/ACTIVE).
 * Tüketiciler {@code status} alanına göre dallanır (mail içeriği, ürün satılabilirliği).
 * {@code isVerified} additive eklendi — arama görünürlüğü ACTIVE && isVerified ile belirlenir
 * (eski mesajlarda null gelebilir, tüketici null-safe değerlendirir).
 */
public record TenantStatusChangedEventPayload(
        Long tenantId,
        String name,
        String contactEmail,
        String status,
        Boolean isVerified
) {
}
