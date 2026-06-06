package com.ecommerce.orderservice.order.constant;

public enum OrderStatus {
    CONFIRMED,  // Ödeme alındı, onaylandı — sipariş bu state'te oluşturulur
    SHIPPED,    // Merchant kargoya verdi
    DELIVERED,  // Teslim edildi
    CANCELLED,  // Kullanıcı iptal etti (CONFIRMED iken)
    REFUNDED    // İade edildi (stok commit hatası veya iptal sonrası)
}
