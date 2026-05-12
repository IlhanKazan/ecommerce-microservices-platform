package com.ecommerce.mailservice.mail.service;

public interface MailService {
    void sendTenantActivated(String toEmail, String tenantName, String inboxMessageId);
    void sendTenantPaymentFailed(String toEmail, String tenantName, String inboxMessageId);
    void sendPaymentSuccess(String toEmail, String amount, String currency, String paymentType, String inboxMessageId);
}
