package com.ecommerce.paymentservice.subscription.service.impl;

import com.ecommerce.paymentservice.outbox.service.OutboxService;
import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.service.PaymentService;
import com.ecommerce.paymentservice.subscription.constant.BillingCycle;
import com.ecommerce.paymentservice.subscription.constant.TenantSubscriptionStatus;
import com.ecommerce.paymentservice.subscription.entity.SubscriptionPlan;
import com.ecommerce.paymentservice.subscription.entity.TenantCard;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import com.ecommerce.paymentservice.subscription.repository.TenantCardRepository;
import com.ecommerce.paymentservice.subscription.repository.TenantSubscriptionRepository;
import com.ecommerce.paymentservice.subscription.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalProcessor {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantCardRepository tenantCardRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final PaymentService paymentService;
    private final OutboxService outboxService;

    @Transactional
    public void processSingleRenewal(TenantSubscription sub) {
        // Döngü sonuna ertelenmiş plan (downgrade) varsa bu yenilemede uygula
        SubscriptionPlan scheduledPlan = resolveScheduledPlan(sub);

        BigDecimal chargeAmount = scheduledPlan != null ? scheduledPlan.getPrice() : sub.getFeeAmount();

        // Ücretsiz plan: iyzico 0₺ çekemez → tahsilatsız uzat
        if (chargeAmount == null || chargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            if (scheduledPlan != null) {
                applyScheduledPlan(sub, scheduledPlan);
            }
            advanceDates(sub);
            sub.setFailedPaymentCount(0);
            outboxService.publishSubscriptionRenewalSuccessEvent(sub);
            tenantSubscriptionRepository.save(sub);
            log.info("Ücretsiz plan yenilemesi (tahsilatsız) — ID: {}", sub.getId());
            return;
        }

        // Tahsilat varsayılan karttan (token + userKey); yoksa eski tek-token alanına düş
        TenantCard defaultCard = tenantCardRepository.findByTenantIdAndIsDefaultTrue(sub.getTenantId()).orElse(null);
        String cardToken = defaultCard != null ? defaultCard.getIyzicoCardToken() : sub.getIyzicoCardToken();
        String cardUserKey = defaultCard != null ? defaultCard.getIyzicoCardUserKey() : sub.getIyzicoCardUserKey();

        if (cardToken == null) {
            log.warn("Kayıtlı kart yok, abonelik iptal ediliyor. ID: {}", sub.getId());
            sub.setStatus(TenantSubscriptionStatus.CANCELED);
            sub.setCancellationReason("Otomatik yenileme için kayıtlı kart bulunamadı.");
            tenantSubscriptionRepository.save(sub);
            return;
        }

        Payment payment = paymentService.processTokenCharge(sub.getTenantId(), cardToken, cardUserKey, chargeAmount);

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            if (scheduledPlan != null) {
                applyScheduledPlan(sub, scheduledPlan);
                log.info("Bekleyen plan geçişi uygulandı — ID: {}, yeni plan: {}", sub.getId(), scheduledPlan.getName());
            }
            log.info("Ödeme başarılı. Abonelik uzatılıyor. ID: {}", sub.getId());
            advanceDates(sub);
            sub.setFailedPaymentCount(0);
            outboxService.publishSubscriptionRenewalSuccessEvent(sub);
            tenantSubscriptionRepository.save(sub);
        } else {
            log.error("Ödeme başarısız. ID: {}, Hata: {}", sub.getId(), payment.getFailureReason());
            handlePaymentFailure(sub, payment.getFailureReason());
        }
    }

    private SubscriptionPlan resolveScheduledPlan(TenantSubscription sub) {
        if (sub.getScheduledPlanId() == null) {
            return null;
        }
        SubscriptionPlan plan = subscriptionPlanService.findByIdAndIsActive(sub.getScheduledPlanId(), true).orElse(null);
        if (plan == null) {
            log.warn("Bekleyen plan bulunamadı/aktif değil, temizleniyor. ID: {}, scheduledPlanId: {}",
                    sub.getId(), sub.getScheduledPlanId());
            sub.setScheduledPlanId(null);
        }
        return plan;
    }

    private void applyScheduledPlan(TenantSubscription sub, SubscriptionPlan plan) {
        sub.setPlanName(plan.getName());
        sub.setFeeAmount(plan.getPrice());
        sub.setCommissionRate(plan.getCommissionRate());
        sub.setCycleUnit(plan.getBillingCycle());
        sub.setScheduledPlanId(null);
    }

    private void advanceDates(TenantSubscription sub) {
        sub.setLastSuccessfulPaymentDate(LocalDateTime.now());
        if (sub.getCycleUnit() == BillingCycle.MONTHLY) {
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
