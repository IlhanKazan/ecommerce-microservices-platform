package com.ecommerce.paymentservice.subscription.service.impl;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.paymentservice.common.exception.ConflictException;
import com.ecommerce.paymentservice.payment.adapter.IyzicoCardAdapter;
import com.ecommerce.paymentservice.payment.domain.AddCardCommand;
import com.ecommerce.paymentservice.payment.domain.IyzicoStoredCard;
import com.ecommerce.paymentservice.subscription.constant.TenantSubscriptionStatus;
import com.ecommerce.paymentservice.subscription.entity.TenantCard;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import com.ecommerce.paymentservice.subscription.repository.TenantCardRepository;
import com.ecommerce.paymentservice.subscription.repository.TenantSubscriptionRepository;
import com.ecommerce.paymentservice.subscription.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private final TenantCardRepository tenantCardRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final IyzicoCardAdapter iyzicoCardAdapter;

    @Override
    @Transactional
    public TenantCard addCard(Long tenantId, String email, AddCardCommand command) {
        String existingUserKey = resolveCardUserKey(tenantId);
        // iyzico ilk kartta (yeni cüzdan) geçerli formatta e-posta ister.
        // Öncelik: oturum açan kullanıcının JWT e-postası → abonelik contactEmail → güvenli fallback.
        String resolvedEmail = (email != null && !email.isBlank()) ? email : resolveEmail(tenantId);

        IyzicoStoredCard stored = iyzicoCardAdapter.storeCard(existingUserKey, resolvedEmail, command);

        boolean firstCard = tenantCardRepository.countByTenantId(tenantId) == 0;

        TenantCard card = TenantCard.builder()
                .tenantId(tenantId)
                .iyzicoCardToken(stored.cardToken())
                .iyzicoCardUserKey(stored.cardUserKey())
                .cardAlias(command.cardAlias())
                .lastFour(stored.lastFour())
                .cardAssociation(stored.cardAssociation())
                .cardFamily(stored.cardFamily())
                .binNumber(stored.binNumber())
                .isDefault(firstCard)
                .build();

        TenantCard saved = tenantCardRepository.save(card);
        if (firstCard) {
            syncSubscriptionDefaultCard(tenantId, saved);
        }
        log.info("Kart eklendi — tenantId: {}, cardId: {}, default: {}", tenantId, saved.getId(), firstCard);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantCard> listCards(Long tenantId) {
        return tenantCardRepository.findAllByTenantIdOrderByIsDefaultDescCreatedAtDesc(tenantId);
    }

    @Override
    @Transactional
    public void deleteCard(Long tenantId, Long cardId) {
        TenantCard card = tenantCardRepository.findByIdAndTenantId(cardId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Kart bulunamadı.", "CARD_NOT_FOUND"));

        if (card.isDefault()) {
            Optional<TenantCard> replacement = tenantCardRepository
                    .findAllByTenantIdOrderByIsDefaultDescCreatedAtDesc(tenantId).stream()
                    .filter(c -> !c.getId().equals(cardId))
                    .findFirst();

            if (replacement.isEmpty() && hasActivePaidSubscription(tenantId)) {
                throw new ConflictException(
                        "Aktif ücretli aboneliğinizin tek kartını silemezsiniz. Önce başka bir kart ekleyin.",
                        "LAST_CARD_ON_ACTIVE_SUBSCRIPTION");
            }

            replacement.ifPresent(r -> {
                r.setDefault(true);
                tenantCardRepository.save(r);
                syncSubscriptionDefaultCard(tenantId, r);
            });
        }

        iyzicoCardAdapter.deleteCard(card.getIyzicoCardUserKey(), card.getIyzicoCardToken());
        tenantCardRepository.delete(card);
        log.info("Kart silindi — tenantId: {}, cardId: {}", tenantId, cardId);
    }

    @Override
    @Transactional
    public void setDefault(Long tenantId, Long cardId) {
        TenantCard target = tenantCardRepository.findByIdAndTenantId(cardId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Kart bulunamadı.", "CARD_NOT_FOUND"));

        if (target.isDefault()) {
            return;
        }

        tenantCardRepository.findByTenantIdAndIsDefaultTrue(tenantId).ifPresent(old -> {
            old.setDefault(false);
            tenantCardRepository.save(old);
        });

        target.setDefault(true);
        tenantCardRepository.save(target);
        syncSubscriptionDefaultCard(tenantId, target);
        log.info("Varsayılan kart değişti — tenantId: {}, cardId: {}", tenantId, cardId);
    }

    @Override
    @Transactional
    public void seedDefaultCard(Long tenantId, Long customerId, IyzicoStoredCard storedCard) {
        if (storedCard == null || storedCard.cardToken() == null) {
            return;
        }
        if (tenantCardRepository.countByTenantId(tenantId) > 0) {
            return;
        }
        TenantCard card = TenantCard.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .iyzicoCardToken(storedCard.cardToken())
                .iyzicoCardUserKey(storedCard.cardUserKey())
                .cardAlias("Abonelik kartı")
                .lastFour(storedCard.lastFour())
                .cardAssociation(storedCard.cardAssociation())
                .cardFamily(storedCard.cardFamily())
                .binNumber(storedCard.binNumber())
                .isDefault(true)
                .build();
        tenantCardRepository.save(card);
        log.info("Abonelik kartı varsayılan olarak vault'a kaydedildi — tenantId: {}", tenantId);
    }

    private boolean hasActivePaidSubscription(Long tenantId) {
        return tenantSubscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId)
                .filter(s -> s.getStatus() == TenantSubscriptionStatus.ACTIVE)
                .filter(TenantSubscription::isAutoRenew)
                .map(s -> s.getFeeAmount() != null && s.getFeeAmount().compareTo(BigDecimal.ZERO) > 0)
                .orElse(false);
    }

    private void syncSubscriptionDefaultCard(Long tenantId, TenantCard card) {
        tenantSubscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId).ifPresent(sub -> {
            sub.setIyzicoCardToken(card.getIyzicoCardToken());
            sub.setIyzicoCardUserKey(card.getIyzicoCardUserKey());
            tenantSubscriptionRepository.save(sub);
        });
    }

    private String resolveCardUserKey(Long tenantId) {
        return tenantCardRepository.findFirstByTenantId(tenantId)
                .map(TenantCard::getIyzicoCardUserKey)
                .orElseGet(() -> tenantSubscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId)
                        .map(TenantSubscription::getIyzicoCardUserKey)
                        .orElse(null));
    }

    private String resolveEmail(Long tenantId) {
        return tenantSubscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId)
                .map(TenantSubscription::getContactEmail)
                .filter(e -> e != null && !e.isBlank())
                .orElse("tenant-" + tenantId + "@ilhankazan-ecommerce.com");
    }
}
