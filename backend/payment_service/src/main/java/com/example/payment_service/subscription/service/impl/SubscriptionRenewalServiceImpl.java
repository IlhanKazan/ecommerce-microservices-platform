package com.example.payment_service.subscription.service.impl;

import com.example.payment_service.payment.constant.PaymentStatus;
import com.example.payment_service.payment.entity.Payment;
import com.example.payment_service.payment.service.PaymentService;
import com.example.payment_service.subscription.constant.TenantSubscriptionStatus;
import com.example.payment_service.subscription.entity.TenantSubscription;
import com.example.payment_service.subscription.repository.TenantSubscriptionRepository;
import com.example.payment_service.subscription.service.SubscriptionRenewalService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class SubscriptionRenewalServiceImpl implements SubscriptionRenewalService {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final PaymentService paymentService;

    public SubscriptionRenewalServiceImpl(TenantSubscriptionRepository subscriptionRepository, PaymentService paymentService) {
        this.tenantSubscriptionRepository = subscriptionRepository;
        this.paymentService = paymentService;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void processDailyRenewals() {
        log.info("Günlük abonelik yenileme kontrolü başladı...");

        // Yarın veya bugün ödemesi gereken aktif abonelikleri bul
        List<TenantSubscription> dueSubscriptions = tenantSubscriptionRepository
                .findAllByNextBillingDateBeforeAndStatus(LocalDate.now().plusDays(1), TenantSubscriptionStatus.ACTIVE);

        log.info("{} adet yenilenecek abonelik bulundu.", dueSubscriptions.size());

        for (TenantSubscription sub : dueSubscriptions) {
            try {
                // TODO [08.02.2026 22:30]: @Transactional self-invocation cozulecek
                // Her abonelik için ayrı transaction. Biri patlarsa diğerleri etkilenmesin
                processSingleRenewal(sub);
            } catch (Exception e) {
                log.error("Abonelik yenileme hatası ID: " + sub.getId(), e);
            }
        }
    }

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
    }
}
