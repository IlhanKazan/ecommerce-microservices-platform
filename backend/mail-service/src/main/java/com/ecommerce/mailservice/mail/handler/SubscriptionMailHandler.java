package com.ecommerce.mailservice.mail.handler;

import com.ecommerce.contracts.event.payment.SubscriptionActivatedEventPayload;
import com.ecommerce.contracts.event.payment.SubscriptionPlanChangedEventPayload;
import com.ecommerce.contracts.event.payment.SubscriptionRenewalFailedEventPayload;
import com.ecommerce.contracts.event.payment.SubscriptionRenewalSuccessEventPayload;
import com.ecommerce.mailservice.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionMailHandler {

    private final MailService mailService;

    public void handleSubscriptionActivated(SubscriptionActivatedEventPayload payload, String messageId) {
        if (payload.contactEmail() == null) {
            log.warn("SUBSCRIPTION_ACTIVATED — contactEmail null, mail atlanıyor. tenantId: {}", payload.tenantId());
            return;
        }
        log.info("Abonelik aktivasyon maili — tenantId: {}, email: {}", payload.tenantId(), payload.contactEmail());
        mailService.sendSubscriptionActivated(payload.contactEmail(), payload.planName(), payload.nextBillingDate(), messageId);
    }

    public void handleRenewalSuccess(SubscriptionRenewalSuccessEventPayload payload, String messageId) {
        if (payload.contactEmail() == null) {
            log.warn("SUBSCRIPTION_RENEWAL_SUCCESS — contactEmail null, mail atlanıyor. tenantId: {}", payload.tenantId());
            return;
        }
        log.info("Abonelik yenileme başarı maili — tenantId: {}", payload.tenantId());
        mailService.sendSubscriptionRenewalSuccess(payload.contactEmail(), payload.planName(), payload.nextBillingDate(), messageId);
    }

    public void handleRenewalFailed(SubscriptionRenewalFailedEventPayload payload, String messageId) {
        if (payload.contactEmail() == null) {
            log.warn("SUBSCRIPTION_RENEWAL_FAILED — contactEmail null, mail atlanıyor. tenantId: {}", payload.tenantId());
            return;
        }
        log.info("Abonelik yenileme başarısız maili — tenantId: {}, deneme: {}, askıya alındı: {}",
                payload.tenantId(), payload.failedAttempts(), payload.suspended());
        mailService.sendSubscriptionRenewalFailed(
                payload.contactEmail(), payload.planName(), payload.failureReason(),
                payload.failedAttempts(), payload.suspended(), messageId);
    }

    public void handlePlanChanged(SubscriptionPlanChangedEventPayload payload, String messageId) {
        if (payload.contactEmail() == null) {
            log.warn("SUBSCRIPTION_PLAN_CHANGED — contactEmail null, mail atlanıyor. tenantId: {}", payload.tenantId());
            return;
        }
        log.info("Plan değişikliği maili — tenantId: {}, tip: {}, {} → {}",
                payload.tenantId(), payload.changeType(), payload.oldPlanName(), payload.newPlanName());
        mailService.sendSubscriptionPlanChanged(
                payload.contactEmail(), payload.oldPlanName(), payload.newPlanName(),
                payload.changeType(), payload.effectiveDate(), payload.chargedAmount(), messageId);
    }
}
