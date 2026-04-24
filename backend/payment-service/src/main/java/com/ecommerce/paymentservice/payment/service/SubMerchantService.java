package com.ecommerce.paymentservice.payment.service;

import com.ecommerce.paymentservice.payment.controller.dto.request.SubMerchantCreateRequest;
import com.ecommerce.paymentservice.payment.controller.dto.request.SubMerchantUpdateRequest;

public interface SubMerchantService {
    String createSubMerchant(SubMerchantCreateRequest dto);
    String updateSubMerchant(SubMerchantUpdateRequest dto);
}
