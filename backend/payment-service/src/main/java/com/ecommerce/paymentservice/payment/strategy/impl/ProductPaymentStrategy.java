package com.ecommerce.paymentservice.payment.strategy.impl;

import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;
import com.ecommerce.paymentservice.payment.strategy.PaymentStrategy;
import com.iyzipay.request.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ProductPaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(PaymentType type) {
        return type == PaymentType.PRODUCT_ORDER;
    }

    // TODO [08.02.2026 22:25]: Gidip OrderService'den sipariş tutarını öğrenmeli, şimdilik mock data donuyoruz
    @Override
    public BigDecimal calculatePrice(Long orderId) {
        return BigDecimal.ZERO;
    }

    // TODO [08.02.2026 22:25]: Burada BasketItemType.PHYSICAL olur ve Kargo adresi zorunlu olur
    @Override
    public CreatePaymentRequest prepareIyzicoRequest(Payment payment, PaymentContext context) {
        return new CreatePaymentRequest();
    }

    // TODO [08.02.2026 22:25]: product odemeleriyle alakasi yok o yuzden bos, duzenlenebilir
    @Override
    public CreatePaymentRequest prepareRenewalRequest(Payment payment, String cardToken) {
        return null;
    }
}
