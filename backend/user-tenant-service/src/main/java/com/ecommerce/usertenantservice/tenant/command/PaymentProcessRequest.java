package com.ecommerce.usertenantservice.tenant.command;

import com.ecommerce.usertenantservice.tenant.constant.PaymentType;

public record PaymentProcessRequest(
        PaymentType type,
        Long referenceId,
        Long customerId,
        Long tenantId,
        PaymentCardInfo cardInfo,
        BuyerInfo buyer,
        AddressInfo billingAddress,
        AddressInfo shippingAddress
) {}