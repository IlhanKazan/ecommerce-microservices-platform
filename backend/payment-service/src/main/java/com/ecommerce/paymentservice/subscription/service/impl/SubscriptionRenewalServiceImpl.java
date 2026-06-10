package com.ecommerce.paymentservice.subscription.service.impl;

import com.ecommerce.paymentservice.subscription.constant.TenantSubscriptionStatus;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import com.ecommerce.paymentservice.subscription.repository.TenantSubscriptionRepository;
import com.ecommerce.paymentservice.subscription.service.SubscriptionRenewalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalServiceImpl implements SubscriptionRenewalService {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final SubscriptionRenewalProcessor renewalProcessor;

    @Scheduled(cron = "0 0 3 * * ?")
    public void processDailyRenewals() {
        log.info("Günlük abonelik yenileme kontrolü başladı...");

        List<TenantSubscription> dueSubscriptions = tenantSubscriptionRepository
                .findAllByNextBillingDateBeforeAndStatus(LocalDate.now().plusDays(1), TenantSubscriptionStatus.ACTIVE);

        log.info("{} adet yenilenecek abonelik bulundu.", dueSubscriptions.size());

        for (TenantSubscription sub : dueSubscriptions) {
            try {
                renewalProcessor.processSingleRenewal(sub);
            } catch (Exception e) {
                log.error("Abonelik yenileme hatası ID: " + sub.getId(), e);
            }
        }
    }
}
