package com.ecommerce.paymentservice.subscription.domain;

public enum PlanChangeType {
    /** Üst plana anında geçildi, prorate fark tahsil edildi. */
    UPGRADE,
    /** Alt plana geçiş döngü sonunda uygulanacak; şimdilik tahsilat yok. */
    DOWNGRADE_SCHEDULED,
    /** Bekleyen alt-plan geçişi iptal edildi (kullanıcı mevcut planında kaldı). */
    DOWNGRADE_CANCELED
}
