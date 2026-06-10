package com.ecommerce.paymentservice.payment.service;

import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * iyzico ödemesi başarılı olduktan (para çekildikten) sonra abonelik provisioning'i
 * (createActiveSubscription / outbox) patlarsa para izinin sessizce kaybolmasını önler.
 *
 * REQUIRES_NEW ile AYRI bir transaction'da kalıcı bir {@code PROVISION_FAILED} kaydı yazar;
 * ana işlem rollback olsa bile bu kayıt commit'lenir (IyzicoTransactionServiceImpl ile aynı desen).
 * Ayrı bean olması şart — self-invocation REQUIRES_NEW'i proxy üzerinden atlatamaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProvisioningFailureRecorder {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordProvisionFailure(Long tenantId,
                                       Long customerId,
                                       Long subscriptionId,
                                       BigDecimal amount,
                                       String iyzicoTxnId,
                                       String reason) {
        Payment ghost = Payment.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .subscriptionId(subscriptionId)
                .amount(amount)
                .netAmount(amount)
                .currency("TRY")
                .paymentType(PaymentType.SUBSCRIPTION)
                .paymentStatus(PaymentStatus.PROVISION_FAILED)
                .iyzicoTransactionId(iyzicoTxnId)
                .failureReason(reason)
                .failedAt(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(ghost);
        log.error("[GHOST-PAYMENT] PROVISION_FAILED kaydı yazıldı — paymentId: {}, tenantId: {}, iyzicoTxnId: {}. " +
                        "Para çekildi ama abonelik oluşturulamadı; telafi (refund/retry) gerekli.",
                saved.getId(), tenantId, iyzicoTxnId);
    }
}
