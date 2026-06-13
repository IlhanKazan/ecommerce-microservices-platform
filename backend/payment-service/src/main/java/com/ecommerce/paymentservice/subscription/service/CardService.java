package com.ecommerce.paymentservice.subscription.service;

import com.ecommerce.paymentservice.payment.domain.AddCardCommand;
import com.ecommerce.paymentservice.payment.domain.IyzicoStoredCard;
import com.ecommerce.paymentservice.subscription.entity.TenantCard;

import java.util.List;

public interface CardService {

    TenantCard addCard(Long tenantId, String email, AddCardCommand command);

    List<TenantCard> listCards(Long tenantId);

    void deleteCard(Long tenantId, Long cardId);

    void setDefault(Long tenantId, Long cardId);

    /**
     * İlk abonelik ödemesinde iyzico'nun registerCard ile döndürdüğü kartı varsayılan kart olarak
     * vault'a kaydeder. Tenant'ın zaten kartı varsa no-op. Best-effort — patlasa da ödemeyi bozmamalı.
     */
    void seedDefaultCard(Long tenantId, Long customerId, IyzicoStoredCard storedCard);
}
