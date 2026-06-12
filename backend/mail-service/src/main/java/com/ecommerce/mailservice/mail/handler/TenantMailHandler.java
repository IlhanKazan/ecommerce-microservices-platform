package com.ecommerce.mailservice.mail.handler;

import com.ecommerce.contracts.event.tenant.TenantActivatedEventPayload;
import com.ecommerce.contracts.event.tenant.TenantPaymentFailedEventPayload;
import com.ecommerce.contracts.event.tenant.TenantStatusChangedEventPayload;
import com.ecommerce.mailservice.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantMailHandler {

    private final MailService mailService;

    public void handleTenantActivated(TenantActivatedEventPayload payload, String messageId) {
        log.info("Tenant aktivasyon maili gönderiliyor — tenantId: {}, email: {}",
                payload.tenantId(), payload.contactEmail());
        mailService.sendTenantActivated(payload.contactEmail(), payload.name(), messageId);
    }

    public void handleTenantPaymentFailed(TenantPaymentFailedEventPayload payload, String messageId) {
        log.info("Tenant ödeme başarısız maili gönderiliyor — tenantId: {}, email: {}",
                payload.tenantId(), payload.contactEmail());
        mailService.sendTenantPaymentFailed(payload.contactEmail(), payload.name(), messageId);
    }

    public void handleTenantStatusChanged(TenantStatusChangedEventPayload payload, String messageId) {
        log.info("Tenant durum değişikliği maili — tenantId: {}, status: {}, email: {}",
                payload.tenantId(), payload.status(), payload.contactEmail());

        if (payload.contactEmail() == null) {
            log.warn("Tenant durum maili atlandı — contactEmail null. tenantId: {}", payload.tenantId());
            return;
        }

        switch (payload.status()) {
            case "PASSIVE" -> mailService.sendTenantPaused(payload.contactEmail(), payload.name(), messageId);
            case "CLOSED" -> mailService.sendTenantClosed(payload.contactEmail(), payload.name(), messageId);
            case "ACTIVE" -> mailService.sendTenantReactivated(payload.contactEmail(), payload.name(), messageId);
            default -> log.debug("Mail gerektirmeyen tenant status: {}", payload.status());
        }
    }
}
