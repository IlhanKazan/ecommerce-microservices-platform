package com.ecommerce.paymentservice.subscription.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.paymentservice.common.exception.ConflictException;
import com.ecommerce.paymentservice.outbox.service.OutboxService;
import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.service.PaymentService;
import com.ecommerce.paymentservice.subscription.constant.BillingCycle;
import com.ecommerce.paymentservice.subscription.constant.TenantSubscriptionStatus;
import com.ecommerce.paymentservice.subscription.domain.ChangePlanResult;
import com.ecommerce.paymentservice.subscription.domain.PlanChangeType;
import com.ecommerce.paymentservice.subscription.entity.SubscriptionPlan;
import com.ecommerce.paymentservice.subscription.entity.TenantCard;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import com.ecommerce.paymentservice.subscription.repository.TenantCardRepository;
import com.ecommerce.paymentservice.subscription.repository.TenantSubscriptionRepository;
import com.ecommerce.paymentservice.subscription.service.ChangePlanService;
import com.ecommerce.paymentservice.subscription.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChangePlanServiceImpl implements ChangePlanService {

    /** Bu tutarın altındaki prorate farkları tahsilatsız geçilir (örn. billing günü bugünse). */
    private static final BigDecimal MIN_CHARGE = new BigDecimal("0.50");

    private final TenantSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final TenantCardRepository tenantCardRepository;
    private final PaymentService paymentService;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public ChangePlanResult changePlan(Long tenantId, Long newPlanId) {
        TenantSubscription sub = subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId)
                .orElseThrow(() -> new ConflictException(
                        "Aktif aboneliğiniz yok. Önce bir plana abone olun.", "NO_ACTIVE_SUBSCRIPTION"));

        if (sub.getStatus() != TenantSubscriptionStatus.ACTIVE) {
            throw new ConflictException("Aboneliğiniz aktif değil, plan değiştirilemez.", "SUBSCRIPTION_NOT_ACTIVE");
        }

        SubscriptionPlan newPlan = subscriptionPlanService.findByIdAndIsActive(newPlanId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Plan bulunamadı.", "PLAN_NOT_FOUND"));

        // Aynı plan: bekleyen downgrade varsa iptal et, yoksa hata
        if (newPlan.getName().equals(sub.getPlanName())) {
            if (sub.getScheduledPlanId() != null) {
                sub.setScheduledPlanId(null);
                subscriptionRepository.save(sub);
                log.info("Bekleyen downgrade iptal edildi — tenantId: {}", tenantId);
                return new ChangePlanResult(PlanChangeType.DOWNGRADE_CANCELED, BigDecimal.ZERO,
                        sub.getNextBillingDate(), sub.getPlanName());
            }
            throw new BusinessException("Zaten bu plandasınız.", "ALREADY_ON_PLAN");
        }

        int cmp = newPlan.getPrice().compareTo(sub.getFeeAmount());
        if (cmp > 0) {
            return handleUpgrade(sub, newPlan);
        }
        // newPrice <= currentFee → downgrade (eşit fiyat farklı plan da döngü sonuna ertelenir)
        return scheduleDowngrade(sub, newPlan);
    }

    private ChangePlanResult handleUpgrade(TenantSubscription sub, SubscriptionPlan newPlan) {
        String oldPlanName = sub.getPlanName();
        boolean sameCycle = newPlan.getBillingCycle() == sub.getCycleUnit();

        BigDecimal chargeAmount;
        LocalDate newNextBillingDate;
        if (sameCycle) {
            chargeAmount = computeProratedDifference(sub, newPlan.getPrice());
            newNextBillingDate = sub.getNextBillingDate(); // billing tarihi sabit
        } else {
            // Cross-cycle (ör. MONTHLY→YEARLY): prorate yerine tam fiyat + döngü sıfırla
            chargeAmount = newPlan.getPrice();
            newNextBillingDate = newPlan.getBillingCycle() == BillingCycle.MONTHLY
                    ? LocalDate.now().plusMonths(1) : LocalDate.now().plusYears(1);
        }

        if (chargeAmount.compareTo(MIN_CHARGE) >= 0) {
            TenantCard card = tenantCardRepository.findByTenantIdAndIsDefaultTrue(sub.getTenantId())
                    .orElseThrow(() -> new ConflictException(
                            "Üst plana geçmek için kayıtlı bir kart eklemeniz gerekiyor.", "NO_DEFAULT_CARD"));

            Payment payment = paymentService.processTokenCharge(
                    sub.getTenantId(), card.getIyzicoCardToken(), card.getIyzicoCardUserKey(), chargeAmount);

            if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
                throw new BusinessException(
                        "Ödeme alınamadı: " + (payment.getFailureReason() != null ? payment.getFailureReason() : "bilinmeyen hata"),
                        "PLAN_CHANGE_PAYMENT_FAILED");
            }
            log.info("Upgrade tahsilatı başarılı — tenantId: {}, tutar: {}", sub.getTenantId(), chargeAmount);
        } else {
            chargeAmount = BigDecimal.ZERO;
        }

        applyPlan(sub, newPlan);
        sub.setNextBillingDate(newNextBillingDate);
        sub.setScheduledPlanId(null); // varsa bekleyen downgrade temizlenir
        sub.setLastSuccessfulPaymentDate(java.time.LocalDateTime.now());
        subscriptionRepository.save(sub);

        outboxService.publishSubscriptionPlanChangedEvent(sub, oldPlanName, newPlan.getName(),
                PlanChangeType.UPGRADE.name(), LocalDate.now(), chargeAmount);

        return new ChangePlanResult(PlanChangeType.UPGRADE, chargeAmount, LocalDate.now(), newPlan.getName());
    }

    private ChangePlanResult scheduleDowngrade(TenantSubscription sub, SubscriptionPlan newPlan) {
        sub.setScheduledPlanId(newPlan.getId());
        subscriptionRepository.save(sub);
        log.info("Downgrade planlandı — tenantId: {}, yeni plan: {}, geçerlilik: {}",
                sub.getTenantId(), newPlan.getName(), sub.getNextBillingDate());

        outboxService.publishSubscriptionPlanChangedEvent(sub, sub.getPlanName(), newPlan.getName(),
                PlanChangeType.DOWNGRADE_SCHEDULED.name(), sub.getNextBillingDate(), BigDecimal.ZERO);

        return new ChangePlanResult(PlanChangeType.DOWNGRADE_SCHEDULED, BigDecimal.ZERO,
                sub.getNextBillingDate(), newPlan.getName());
    }

    /**
     * Kalan güne göre fiyat farkı: (newPrice - currentFee) * kalanGün / döngüGünü.
     */
    private BigDecimal computeProratedDifference(TenantSubscription sub, BigDecimal newPrice) {
        LocalDate today = LocalDate.now();
        LocalDate nextBilling = sub.getNextBillingDate();
        LocalDate previousBilling = sub.getCycleUnit() == BillingCycle.MONTHLY
                ? nextBilling.minusMonths(1) : nextBilling.minusYears(1);

        long totalCycleDays = ChronoUnit.DAYS.between(previousBilling, nextBilling);
        long remainingDays = Math.max(0, ChronoUnit.DAYS.between(today, nextBilling));

        if (totalCycleDays <= 0 || remainingDays <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal priceDiff = newPrice.subtract(sub.getFeeAmount());
        return priceDiff
                .multiply(BigDecimal.valueOf(remainingDays))
                .divide(BigDecimal.valueOf(totalCycleDays), 2, RoundingMode.HALF_UP);
    }

    private void applyPlan(TenantSubscription sub, SubscriptionPlan plan) {
        sub.setPlanName(plan.getName());
        sub.setFeeAmount(plan.getPrice());
        sub.setCommissionRate(plan.getCommissionRate());
        sub.setCycleUnit(plan.getBillingCycle());
    }
}
