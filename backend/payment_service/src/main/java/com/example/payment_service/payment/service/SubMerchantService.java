package com.example.payment_service.payment.service;

import com.example.payment_service.payment.controller.dto.request.SubMerchantCreateRequest;
import com.example.payment_service.payment.controller.dto.request.SubMerchantUpdateRequest;

public interface SubMerchantService {
    String createSubMerchant(SubMerchantCreateRequest dto);
    String updateSubMerchant(SubMerchantUpdateRequest dto);
}
