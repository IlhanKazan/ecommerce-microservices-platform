package com.example.payment_service.payment.strategy.impl;

import com.example.payment_service.payment.entity.Payment;
import com.example.payment_service.payment.constant.PaymentType;
import com.example.payment_service.payment.domain.PaymentContext;
import com.example.payment_service.payment.strategy.PaymentStrategy;
import com.example.payment_service.subscription.entity.SubscriptionPlan;
import com.example.payment_service.subscription.service.SubscriptionPlanService;
import com.iyzipay.model.Locale;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.Currency;
import com.iyzipay.model.PaymentGroup;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Address;
import com.iyzipay.request.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionPaymentStrategy implements PaymentStrategy {

    private final SubscriptionPlanService planService;

    @Override
    public boolean supports(PaymentType type) {
        return type == PaymentType.SUBSCRIPTION;
    }

    @Override
    public BigDecimal calculatePrice(Long planId) {
        // TODO [27.12.2025 22:47]: altta bahsettigim order fiyat bilgisi kismi burasi
        // TODO [08.02.2026 22:28]: Plan ID ile fiyatı çekiyoruz, planId nereden geliyor kontrol edilecek
        return planService.findByIdAndIsActive(planId, true)
                .map(SubscriptionPlan::getPrice)
                .orElseThrow(() -> new RuntimeException("Plan bulunamadı: " + planId));
    }

    @Override
    public CreatePaymentRequest prepareIyzicoRequest(Payment payment, PaymentContext context) {
        System.out.println(context.getCardInfo());
        // TODO [27.12.2025 22:46]: Burada referenceId subscription planId yerine geciyor. Ama order odemesi oldugunda orderServiceten fiyat bilgisi vs cekilebilir
        String planName = planService.findByIdAndIsActive(context.getReferenceId(), true)
                .map(SubscriptionPlan::getName)
                .orElse("Abonelik");

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(UUID.randomUUID().toString());
        request.setPrice(payment.getAmount());
        request.setPaidPrice(payment.getAmount());
        request.setCurrency(Currency.TRY.name());
        request.setPaymentGroup(PaymentGroup.SUBSCRIPTION.name());

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

        Address billingAddr = new Address();
        billingAddr.setContactName(context.getBillingAddress().contactName());
        billingAddr.setCity(context.getBillingAddress().city());
        billingAddr.setCountry(context.getBillingAddress().country());
        billingAddr.setAddress(context.getBillingAddress().fullAddress());
        billingAddr.setZipCode(context.getBillingAddress().zipCode());
        request.setBillingAddress(billingAddr);

        Address shippingAddr = new Address();
        shippingAddr.setContactName(context.getShippingAddress().contactName());
        shippingAddr.setCity(context.getShippingAddress().city());
        shippingAddr.setCountry(context.getShippingAddress().country());
        shippingAddr.setAddress(context.getShippingAddress().fullAddress());
        shippingAddr.setZipCode(context.getShippingAddress().zipCode());
        request.setShippingAddress(shippingAddr);

        BasketItem item = new BasketItem();

        item.setId(context.getReferenceId().toString());

        item.setName(planName);
        item.setCategory1("Abonelik");
        item.setItemType(BasketItemType.VIRTUAL.name());
        item.setPrice(payment.getAmount());
        request.setBasketItems(Collections.singletonList(item));


        com.iyzipay.model.PaymentCard paymentCard = new com.iyzipay.model.PaymentCard();
        paymentCard.setCardHolderName(context.getCardInfo().holderName());
        paymentCard.setCardNumber(context.getCardInfo().number());
        paymentCard.setExpireMonth(context.getCardInfo().expireMonth());
        paymentCard.setExpireYear(context.getCardInfo().expireYear());
        paymentCard.setCvc(context.getCardInfo().cvc());
        paymentCard.setRegisterCard(1);

        request.setPaymentCard(paymentCard);

        return request;
    }

    // Override yok cunku normal product odemelerinde boyle bir is yok, yani interfacede yok bu metot
    public CreatePaymentRequest prepareRenewalRequest(Payment payment, String cardToken) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(UUID.randomUUID().toString());
        request.setPrice(payment.getAmount());
        request.setPaidPrice(payment.getAmount());
        request.setCurrency(Currency.TRY.name());
        request.setPaymentGroup(PaymentGroup.SUBSCRIPTION.name());

        com.iyzipay.model.PaymentCard paymentCard = new com.iyzipay.model.PaymentCard();
        paymentCard.setCardToken(cardToken);
        request.setPaymentCard(paymentCard);

        // Iyzico zorunlu tutarsa diye dummy data
        Buyer buyer = new Buyer();
        buyer.setId("SYSTEM");
        buyer.setName("Subscription");
        buyer.setSurname("Renewal");
        buyer.setIdentityNumber("11111111111");
        buyer.setEmail("renewal@system.com");
        buyer.setGsmNumber("+905555555555");
        buyer.setRegistrationAddress("Digital Renewal");
        buyer.setCity("Istanbul");
        buyer.setCountry("Turkey");
        buyer.setIp("127.0.0.1");
        request.setBuyer(buyer);

        Address address = new Address();
        address.setContactName("Subscription Renewal");
        address.setCity("Istanbul");
        address.setCountry("Turkey");
        address.setAddress("Digital Service");
        address.setZipCode("34000");
        request.setBillingAddress(address);
        request.setShippingAddress(address);

        BasketItem item = new BasketItem();
        item.setId(payment.getSubscriptionId() != null ? payment.getSubscriptionId().toString() : "RENEWAL");
        item.setName("Abonelik Yenileme");
        item.setCategory1("Abonelik");
        item.setItemType(BasketItemType.VIRTUAL.name());
        item.setPrice(payment.getAmount());
        request.setBasketItems(Collections.singletonList(item));

        return request;
    }
}
