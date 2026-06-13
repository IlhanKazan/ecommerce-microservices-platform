package com.ecommerce.paymentservice.payment.adapter;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.paymentservice.payment.domain.AddCardCommand;
import com.ecommerce.paymentservice.payment.domain.IyzicoStoredCard;
import com.iyzipay.Options;
import com.iyzipay.model.Card;
import com.iyzipay.model.CardInformation;
import com.iyzipay.model.Locale;
import com.iyzipay.request.CreateCardRequest;
import com.iyzipay.request.DeleteCardRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * iyzico Card Storage API sarmalayıcısı. Kart cüzdanı (cardUserKey) + kart token yönetimi.
 * İlk kart eklendiğinde iyzico yeni bir cardUserKey üretir; sonraki kartlar mevcut key ile
 * aynı cüzdana eklenir. Kart numarası/CVC yalnızca buradan iyzico'ya iletilir, saklanmaz.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IyzicoCardAdapter {

    private final Options iyzicoOptions;

    /**
     * @param existingCardUserKey tenant'ın mevcut cüzdan anahtarı; ilk kart için null
     * @param email               yeni cüzdan oluşturulurken iyzico'nun istediği e-posta
     */
    public IyzicoStoredCard storeCard(String existingCardUserKey, String email, AddCardCommand cmd) {
        CreateCardRequest request = new CreateCardRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(UUID.randomUUID().toString());

        if (existingCardUserKey != null && !existingCardUserKey.isBlank()) {
            request.setCardUserKey(existingCardUserKey);
        } else {
            // iyzico ilk kartta (yeni cüzdan) e-posta zorunlu tutar
            request.setEmail(email);
        }

        CardInformation card = new CardInformation();
        card.setCardAlias(cmd.cardAlias());
        card.setCardHolderName(cmd.cardHolderName());
        card.setCardNumber(cmd.cardNumber());
        card.setExpireMonth(cmd.expireMonth());
        card.setExpireYear(cmd.expireYear());
        request.setCard(card);

        Card response = Card.create(request, iyzicoOptions);

        if (!"success".equalsIgnoreCase(response.getStatus())) {
            log.warn("iyzico kart saklama başarısız — code: {}, msg: {}",
                    response.getErrorCode(), response.getErrorMessage());
            throw new BusinessException(
                    response.getErrorMessage() != null ? response.getErrorMessage() : "Kart kaydedilemedi.",
                    "CARD_STORAGE_FAILED");
        }

        return new IyzicoStoredCard(
                response.getCardToken(),
                response.getCardUserKey(),
                response.getLastFourDigits(),
                response.getCardAssociation(),
                response.getCardFamily(),
                response.getBinNumber());
    }

    /**
     * Best-effort: iyzico tarafından silinemese bile loglar ve devam eder — DB kaydı yine silinir,
     * orphan token DB'de kalmaz.
     */
    public void deleteCard(String cardUserKey, String cardToken) {
        try {
            DeleteCardRequest request = new DeleteCardRequest();
            request.setLocale(Locale.TR.getValue());
            request.setConversationId(UUID.randomUUID().toString());
            request.setCardUserKey(cardUserKey);
            request.setCardToken(cardToken);

            Card response = Card.delete(request, iyzicoOptions);
            if (!"success".equalsIgnoreCase(response.getStatus())) {
                log.warn("iyzico kart silme başarısız (DB'den yine silinecek) — code: {}, msg: {}",
                        response.getErrorCode(), response.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("iyzico kart silme hatası (best-effort, DB silme devam eder): {}", e.getMessage(), e);
        }
    }
}
