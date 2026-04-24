/*
package com.ecommerce.usertenantservice.integration.payment.config;

import com.ecommerce.usertenantservice.integration.payment.PaymentServiceClient;
import com.ecommerce.usertenantservice.tenant.controller.dto.request.SubMerchantCreateRequest;
import com.ecommerce.usertenantservice.tenant.controller.dto.request.SubMerchantUpdateRequest;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentHistoryResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.SubMerchantResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantSubscriptionResponse;
import com.ecommerce.usertenantservice.tenant.command.PaymentProcessRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PaymentServiceFallback implements PaymentServiceClient {

    @Override
    public PaymentResponse processPayment(PaymentProcessRequest request){
        return new PaymentResponse(0L, false, "SERVICE_UNAVAILABLE");
    }

    // TODO [19.02.2026 02:42]: Buralara duzenleme yapilacak
    @Override
    public TenantSubscriptionResponse getSubscriptionDetails(Long tenantId) {
        return null;

    }

    @Override
    public Page<PaymentHistoryResponse> getUserPaymentHistory(Long userId, Pageable pageable) {
        return null;
    }

    @Override
    public Page<PaymentHistoryResponse> getTenantPaymentHistory(Long tenantId, Pageable pageable) {
        return null;
    }

    @Override
    public SubMerchantResponse createSubMerchant(SubMerchantCreateRequest request) {
        return "";
    }

    @Override
    public SubMerchantResponse updateSubMerchant(SubMerchantUpdateRequest request) {
        return "";
    }


}
*/
