package com.ecommerce.mailservice.mail.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MailService {
    void sendTenantActivated(String toEmail, String tenantName, String inboxMessageId);
    void sendTenantPaymentFailed(String toEmail, String tenantName, String inboxMessageId);
    void sendTenantPaused(String toEmail, String tenantName, String inboxMessageId);
    void sendTenantClosed(String toEmail, String tenantName, String inboxMessageId);
    void sendTenantReactivated(String toEmail, String tenantName, String inboxMessageId);
    void sendPaymentSuccess(String toEmail, String amount, String currency, String paymentType, String inboxMessageId);

    void sendOrderConfirmed(String toEmail, Long orderId, BigDecimal totalAmount, String currency, String messageId);
    void sendOrderCancelled(String toEmail, Long orderId, String reason, String messageId);
    void sendOrderShipped(String toEmail, Long orderId, String trackingNumber, String messageId);
    void sendOrderRefunded(String toEmail, Long orderId, String reason, String messageId);
    void sendOrderDelivered(String toEmail, Long orderId, String messageId);

    void sendSubscriptionActivated(String toEmail, String planName, LocalDate nextBillingDate, String messageId);
    void sendSubscriptionRenewalSuccess(String toEmail, String planName, LocalDate nextBillingDate, String messageId);
    void sendSubscriptionRenewalFailed(String toEmail, String planName, String failureReason, int failedAttempts, boolean suspended, String messageId);
    void sendSubscriptionPlanChanged(String toEmail, String oldPlanName, String newPlanName, String changeType, LocalDate effectiveDate, BigDecimal chargedAmount, String messageId);
}
