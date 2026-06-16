package com.ecommerce.orderservice.order.constant;

public enum OrderStatus {
    CONFIRMED,        // Ödeme alındı, onaylandı — sipariş bu state'te oluşturulur
    SHIPPED,          // Merchant kargoya verdi
    DELIVERED,        // Teslim edildi
    CANCELLED,        // Kullanıcı iptal etti (CONFIRMED iken)
    REFUNDED,         // İade edildi (stok commit hatası veya iptal sonrası)
    RETURN_REQUESTED, // Müşteri teslim sonrası iade talebi açtı (DELIVERED iken)
    RETURNED,         // İade onaylandı, para iadesi yapıldı (RETURN_REQUESTED iken)
    RETURN_REJECTED   // İade talebi reddedildi — TERMINAL (tekrar talep açılamaz)
}
