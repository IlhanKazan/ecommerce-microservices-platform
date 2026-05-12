package com.ecommerce.mailservice.mail.handler;

import com.ecommerce.contracts.event.payment.PaymentSuccessEventPayload;
import com.ecommerce.mailservice.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentMailHandler {

    private final MailService mailService;

    public void handlePaymentSuccess(PaymentSuccessEventPayload payload, String recipientEmail, String messageId) {
        log.info("Ödeme başarı maili gönderiliyor — paymentId: {}, tenantId: {}",
                payload.paymentId(), payload.tenantId());
        mailService.sendPaymentSuccess(
                recipientEmail,
                payload.amount().toPlainString(),
                payload.currency(),
                payload.paymentType(),
                messageId
        );
    }
}
