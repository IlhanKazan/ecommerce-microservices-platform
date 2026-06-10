package com.ecommerce.paymentservice.subscription.service.impl;

import com.ecommerce.paymentservice.outbox.service.OutboxService;
import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.service.PaymentService;
import com.ecommerce.paymentservice.subscription.constant.TenantSubscriptionStatus;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import com.ecommerce.paymentservice.subscription.repository.TenantSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalProcessor {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final PaymentService paymentService;
    private final OutboxService outboxService;

    @Transactional
    public void processSingleRenewal(TenantSubscription sub) {
        if (sub.getIyzicoCardToken() == null) {
            log.warn("Token yok, abonelik iptal ediliyor. ID: {}", sub.getId());
            sub.setStatus(TenantSubscriptionStatus.CANCELED);
            sub.setCancellationReason("Otomatik yenileme için kart token bulunamadı.");
            tenantSubscriptionRepository.save(sub);
            return;
        }

        Payment payment = paymentService.processRenewalPayment(
                sub.getTenantId(),
                sub.getIyzicoCardToken(),
                sub.getFeeAmount()
        );

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.info("Ödeme başarılı. Abonelik uzatılıyor. ID: {}", sub.getId());
            updateDatesAfterSuccess(sub);
            sub.setFailedPaymentCount(0);
            outboxService.publishSubscriptionRenewalSuccessEvent(sub);
            tenantSubscriptionRepository.save(sub);
        } else {
            log.error("Ödeme başarısız. ID: {}, Hata: {}", sub.getId(), payment.getFailureReason());
            handlePaymentFailure(sub, payment.getFailureReason());
        }
    }

    private void updateDatesAfterSuccess(TenantSubscription sub) {
        sub.setLastSuccessfulPaymentDate(LocalDateTime.now());

        if (sub.getCycleUnit().name().equals("MONTHLY")) {
            sub.setNextBillingDate(sub.getNextBillingDate().plusMonths(1));
        } else {
            sub.setNextBillingDate(sub.getNextBillingDate().plusYears(1));
        }
    }

    private void handlePaymentFailure(TenantSubscription sub, String errorMessage) {
        sub.setFailedPaymentCount(sub.getFailedPaymentCount() + 1);

        if (sub.getFailedPaymentCount() >= 3) {
            sub.setStatus(TenantSubscriptionStatus.SUSPENDED);
            sub.setCancellationReason("Ödeme 3 kez başarısız oldu: " + errorMessage);
        }

        tenantSubscriptionRepository.save(sub);
        boolean suspended = sub.getFailedPaymentCount() >= 3;
        outboxService.publishSubscriptionRenewalFailedEvent(sub, errorMessage, suspended);
    }
}
