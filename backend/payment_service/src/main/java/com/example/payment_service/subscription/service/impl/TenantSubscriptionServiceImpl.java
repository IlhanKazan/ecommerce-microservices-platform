package com.example.payment_service.subscription.service.impl;

import com.example.payment_service.subscription.constant.BillingCycle;
import com.example.payment_service.subscription.constant.TenantSubscriptionStatus;
import com.example.payment_service.subscription.entity.SubscriptionPlan;
import com.example.payment_service.subscription.entity.TenantSubscription;
import com.example.payment_service.subscription.repository.TenantSubscriptionRepository;
import com.example.payment_service.subscription.service.SubscriptionPlanService;
import com.example.payment_service.subscription.service.TenantSubscriptionService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
public class TenantSubscriptionServiceImpl implements TenantSubscriptionService {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final SubscriptionPlanService subscriptionPlanService;

    public TenantSubscriptionServiceImpl(TenantSubscriptionRepository tenantSubscriptionRepository, SubscriptionPlanService subscriptionPlanService) {
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @Override
    public TenantSubscription save(TenantSubscription tenantSubscription){
        return tenantSubscriptionRepository.save(tenantSubscription);
    }

    @Override
    @Transactional
    public TenantSubscription createActiveSubscription(Long tenantId, Long planId, String cardToken, BigDecimal amountPaid) {

        SubscriptionPlan plan = subscriptionPlanService.findByIdAndIsActive(planId, true)
                .orElseThrow(() -> new RuntimeException("Plan bulunamadı"));

        TenantSubscription subscription = TenantSubscription.builder()
                .tenantId(tenantId)
                .planName(plan.getName())
                .feeAmount(amountPaid)
                .cycleUnit(plan.getBillingCycle())
                .status(TenantSubscriptionStatus.ACTIVE)
                .iyzicoCardToken(cardToken)
                .autoRenew(true)
                .startedAt(LocalDateTime.now())
                .lastSuccessfulPaymentDate(LocalDateTime.now())
                .failedPaymentCount(0)
                .build();

        if (plan.getBillingCycle() == BillingCycle.MONTHLY) {
            subscription.setNextBillingDate(LocalDate.now().plusMonths(1));
        } else {
            subscription.setNextBillingDate(LocalDate.now().plusYears(1));
        }

        try {
            TenantSubscription activeSubscription = tenantSubscriptionRepository.save(subscription);
            log.info("Abonelik başarıyla oluşturuldu ve kart token'ı saklandı.");
            return activeSubscription;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public TenantSubscription getSubscriptionDetails(Long tenantId){
        log.info("tenantId: {}", tenantId);
        return tenantSubscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Abonelik bulunamadı"));
    }

    @Override
    public Optional<TenantSubscription> findLatestSubscription(Long tenantId) {
        return tenantSubscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId);
    }


}
