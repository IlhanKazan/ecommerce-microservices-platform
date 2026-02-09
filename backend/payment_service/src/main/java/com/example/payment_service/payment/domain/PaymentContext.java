package com.example.payment_service.payment.domain;

import com.example.payment_service.payment.constant.PaymentType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentContext {
    private PaymentType type;
    private Long referenceId;
    private Long customerId;
    private Long tenantId;
    private PaymentCardInfo cardInfo;
    private BuyerInfo buyer;
    private AddressInfo billingAddress;
    private AddressInfo shippingAddress;
}
