package com.ecommerce.paymentservice.payment.strategy.impl;

import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.domain.AddressInfo;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.strategy.PaymentStrategy;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.Currency;
import com.iyzipay.model.Locale;
import com.iyzipay.model.PaymentGroup;
import com.iyzipay.request.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductPaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(PaymentType type) {
        return type == PaymentType.PRODUCT_ORDER;
    }

    @Override
    public BigDecimal calculatePrice(Long orderId) {
        // context.getAmount() null değilse override edilir (PaymentServiceImpl'de kontrol ediliyor)
        // Bu metot sadece fallback durumunda çağrılır
        return BigDecimal.ZERO;
    }

    @Override
    public CreatePaymentRequest prepareIyzicoRequest(Payment payment, PaymentContext context) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(UUID.randomUUID().toString());
        request.setPrice(payment.getAmount());
        request.setPaidPrice(payment.getAmount());
        request.setCurrency(Currency.TRY.name());
        request.setPaymentGroup(PaymentGroup.PRODUCT.name());

        if (context.getBuyer() != null) {
            Buyer buyer = new Buyer();
            buyer.setId(context.getBuyer().id());
            buyer.setName(context.getBuyer().name());
            buyer.setSurname(context.getBuyer().surname());
            buyer.setGsmNumber(context.getBuyer().gsmNumber());
            buyer.setEmail(context.getBuyer().email());
            buyer.setIdentityNumber(context.getBuyer().identityNumber());
            buyer.setRegistrationAddress(context.getBuyer().fullAddress());
            buyer.setIp(context.getBuyer().ip());
            buyer.setCity(context.getBuyer().city());
            buyer.setCountry(context.getBuyer().country());
            buyer.setZipCode(context.getBuyer().zipCode());
            request.setBuyer(buyer);
        }

        if (context.getBillingAddress() != null) {
            request.setBillingAddress(buildAddress(context.getBillingAddress()));
        }

        if (context.getShippingAddress() != null) {
            request.setShippingAddress(buildAddress(context.getShippingAddress()));
        }

        BasketItem item = new BasketItem();
        item.setId(context.getReferenceId() != null ? context.getReferenceId().toString() : "ORDER");
        item.setName("Sipariş #" + context.getReferenceId());
        item.setCategory1("Ürün");
        item.setItemType(BasketItemType.PHYSICAL.name());
        item.setPrice(payment.getAmount());
        if (context.getSubMerchantKey() != null) {
            item.setSubMerchantKey(context.getSubMerchantKey());
            item.setSubMerchantPrice(payment.getAmount());
        }
        request.setBasketItems(Collections.singletonList(item));

        if (context.getCardInfo() != null) {
            com.iyzipay.model.PaymentCard paymentCard = new com.iyzipay.model.PaymentCard();
            paymentCard.setCardHolderName(context.getCardInfo().holderName());
            paymentCard.setCardNumber(context.getCardInfo().number());
            paymentCard.setExpireMonth(context.getCardInfo().expireMonth());
            paymentCard.setExpireYear(context.getCardInfo().expireYear());
            paymentCard.setCvc(context.getCardInfo().cvc());
            request.setPaymentCard(paymentCard);
        }

        return request;
    }

    @Override
    public CreatePaymentRequest prepareRenewalRequest(Payment payment, String cardToken) {
        return null;
    }

    private Address buildAddress(AddressInfo info) {
        Address addr = new Address();
        addr.setContactName(info.contactName());
        addr.setCity(info.city());
        addr.setCountry(info.country());
        addr.setAddress(info.fullAddress());
        addr.setZipCode(info.zipCode());
        return addr;
    }
}
