package com.ecommerce.mailservice.mail.service;

import java.math.BigDecimal;

public interface MailService {
    void sendTenantActivated(String toEmail, String tenantName, String inboxMessageId);
    void sendTenantPaymentFailed(String toEmail, String tenantName, String inboxMessageId);
    void sendPaymentSuccess(String toEmail, String amount, String currency, String paymentType, String inboxMessageId);

    void sendOrderConfirmed(String toEmail, Long orderId, BigDecimal totalAmount, String currency, String messageId);
    void sendOrderCancelled(String toEmail, Long orderId, String reason, String messageId);
    void sendOrderShipped(String toEmail, Long orderId, String trackingNumber, String messageId);
    void sendOrderRefunded(String toEmail, Long orderId, String reason, String messageId);
}
