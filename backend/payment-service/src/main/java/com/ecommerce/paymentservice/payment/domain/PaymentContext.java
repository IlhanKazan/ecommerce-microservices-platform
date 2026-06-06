package com.ecommerce.paymentservice.payment.domain;

import com.ecommerce.paymentservice.payment.constant.PaymentType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PaymentContext {
    private PaymentType type;
    private Long referenceId;
    private Long customerId;
    private Long tenantId;
    private PaymentCardInfo cardInfo;
    private BuyerInfo buyer;
    private AddressInfo billingAddress;
    private AddressInfo shippingAddress;
    private BigDecimal amount;
    private String currency;
    private String subMerchantKey;
}
