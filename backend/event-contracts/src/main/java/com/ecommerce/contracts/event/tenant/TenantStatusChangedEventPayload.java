package com.ecommerce.contracts.event.tenant;

/**
 * Mağaza yaşam döngüsü durum değişikliği (PASSIVE/CLOSED/ACTIVE).
 * Tüketiciler {@code status} alanına göre dallanır (mail içeriği, ürün satılabilirliği).
 */
public record TenantStatusChangedEventPayload(
        Long tenantId,
        String name,
        String contactEmail,
        String status
) {
}
