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
