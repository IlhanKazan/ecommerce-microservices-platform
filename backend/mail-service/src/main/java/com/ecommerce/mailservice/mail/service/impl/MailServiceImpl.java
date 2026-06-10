package com.ecommerce.mailservice.mail.service.impl;

import com.ecommerce.mailservice.mail.entity.MailLog;
import com.ecommerce.mailservice.mail.entity.MailStatus;
import com.ecommerce.mailservice.mail.repository.MailLogRepository;
import com.ecommerce.mailservice.mail.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailLogRepository mailLogRepository;

    @Value("${mail.from}")
    private String fromAddress;

    @Override
    public void sendTenantActivated(String toEmail, String tenantName, String inboxMessageId) {
        Context ctx = new Context();
        ctx.setVariable("tenantName", tenantName);
        String subject = "Mağazanız Aktif Edildi — " + tenantName;
        send(toEmail, subject, "mail/tenant-activated", ctx, inboxMessageId, "TENANT_ACTIVATED_EVENT");
    }

    @Override
    public void sendTenantPaymentFailed(String toEmail, String tenantName, String inboxMessageId) {
        Context ctx = new Context();
        ctx.setVariable("tenantName", tenantName);
        String subject = "Ödeme Başarısız — " + tenantName;
        send(toEmail, subject, "mail/tenant-payment-failed", ctx, inboxMessageId, "TENANT_PAYMENT_FAILED_EVENT");
    }

    @Override
    public void sendPaymentSuccess(String toEmail, String amount, String currency, String paymentType, String inboxMessageId) {
        Context ctx = new Context();
        ctx.setVariable("amount", amount);
        ctx.setVariable("currency", currency);
        ctx.setVariable("paymentType", paymentType);
        send(toEmail, "Ödemeniz Alındı", "mail/payment-success", ctx, inboxMessageId, "PAYMENT_SUCCESS_EVENT");
    }

    @Override
    public void sendOrderConfirmed(String toEmail, Long orderId, BigDecimal totalAmount, String currency, String messageId) {
        Context ctx = new Context();
        ctx.setVariable("orderId", orderId);
        ctx.setVariable("totalAmount", totalAmount.toPlainString());
        ctx.setVariable("currency", currency);
        send(toEmail, "Siparişiniz Onaylandı — #" + orderId, "mail/order-confirmed", ctx, messageId, "ORDER_CONFIRMED_EVENT");
    }

    @Override
    public void sendOrderCancelled(String toEmail, Long orderId, String reason, String messageId) {
        Context ctx = new Context();
        ctx.setVariable("orderId", orderId);
        ctx.setVariable("reason", reason != null ? reason : "Belirtilmedi");
        send(toEmail, "Siparişiniz İptal Edildi — #" + orderId, "mail/order-cancelled", ctx, messageId, "ORDER_CANCELLED_EVENT");
    }

    @Override
    public void sendOrderShipped(String toEmail, Long orderId, String trackingNumber, String messageId) {
        Context ctx = new Context();
        ctx.setVariable("orderId", orderId);
        ctx.setVariable("trackingNumber", trackingNumber != null ? trackingNumber : "—");
        send(toEmail, "Siparişiniz Kargoya Verildi — #" + orderId, "mail/order-shipped", ctx, messageId, "ORDER_SHIPPED_EVENT");
    }

    @Override
    public void sendOrderRefunded(String toEmail, Long orderId, String reason, String messageId) {
        Context ctx = new Context();
        ctx.setVariable("orderId", orderId);
        ctx.setVariable("reason", reason != null ? reason : "Belirtilmedi");
        send(toEmail, "İadeniz İşleme Alındı — #" + orderId, "mail/order-refunded", ctx, messageId, "ORDER_REFUNDED_EVENT");
    }

    @Override
    public void sendOrderDelivered(String toEmail, Long orderId, String messageId) {
        Context ctx = new Context();
        ctx.setVariable("orderId", orderId);
        send(toEmail, "Siparişiniz Teslim Edildi — #" + orderId, "mail/order-delivered", ctx, messageId, "ORDER_DELIVERED_EVENT");
    }

    @Override
    public void sendSubscriptionActivated(String toEmail, String planName, LocalDate nextBillingDate, String messageId) {
        Context ctx = new Context();
        ctx.setVariable("planName", planName);
        ctx.setVariable("nextBillingDate", nextBillingDate);
        send(toEmail, "Aboneliğiniz Başlatıldı", "mail/subscription-activated", ctx, messageId, "SUBSCRIPTION_ACTIVATED_EVENT");
    }

    @Override
    public void sendSubscriptionRenewalSuccess(String toEmail, String planName, LocalDate nextBillingDate, String messageId) {
        Context ctx = new Context();
        ctx.setVariable("planName", planName);
        ctx.setVariable("nextBillingDate", nextBillingDate);
        send(toEmail, "Aboneliğiniz Yenilendi", "mail/subscription-renewal-success", ctx, messageId, "SUBSCRIPTION_RENEWAL_SUCCESS_EVENT");
    }

    @Override
    public void sendSubscriptionRenewalFailed(String toEmail, String planName, String failureReason, int failedAttempts, boolean suspended, String messageId) {
        Context ctx = new Context();
        ctx.setVariable("planName", planName);
        ctx.setVariable("failureReason", failureReason);
        ctx.setVariable("failedAttempts", failedAttempts);
        ctx.setVariable("suspended", suspended);
        String subject = suspended ? "Aboneliğiniz Askıya Alındı" : "Abonelik Yenileme Başarısız";
        send(toEmail, subject, "mail/subscription-renewal-failed", ctx, messageId, "SUBSCRIPTION_RENEWAL_FAILED_EVENT");
    }

    private void send(String toEmail, String subject, String templateName, Context ctx,
                      String inboxMessageId, String eventType) {
        MailLog logEntry = MailLog.builder()
                .inboxMessageId(inboxMessageId)
                .eventType(eventType)
                .recipientEmail(toEmail)
                .subject(subject)
                .templateName(templateName)
                .status(MailStatus.FAILED)
                .build();

        try {
            String html = templateEngine.process(templateName, ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

            logEntry.setStatus(MailStatus.SENT);
            logEntry.setSentAt(LocalDateTime.now());
            log.info("Mail gönderildi — to: {}, template: {}, inboxId: {}", toEmail, templateName, inboxMessageId);

        } catch (MessagingException e) {
            logEntry.setErrorMessage(e.getMessage());
            log.error("Mail gönderilemedi — to: {}, template: {}, hata: {}", toEmail, templateName, e.getMessage(), e);
            // Exception fırlatılmıyor — mail hatası Kafka consumer'ı retry döngüsüne sokmamalı
        } finally {
            mailLogRepository.save(logEntry);
        }
    }
}
