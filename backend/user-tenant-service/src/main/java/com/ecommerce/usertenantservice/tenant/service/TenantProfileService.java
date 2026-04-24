package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.usertenantservice.integration.payment.PaymentServiceClientAdapter;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentHistoryResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantSubscriptionResponse;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.query.TenantStorefrontInfo;
import com.ecommerce.usertenantservice.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantProfileService {

    private final TenantRepository tenantRepository;
    private final PaymentServiceClientAdapter paymentServiceClientAdapter;
    private static final String NO_MERCHANT_DESCRIPTION = "Mağaza bulunamadı";


    public Tenant getTenantById(Long tenantId) {
        return tenantRepository.findByIdWithDetails(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(NO_MERCHANT_DESCRIPTION, "404"));
    }

    @Transactional
    public void save(Tenant incomingData) {
        tenantRepository.save(incomingData);
    }

    @Transactional
    public Tenant uploadLogo(Long id, String logoUrl){
        Tenant tenant = getTenantById(id);
        tenant.setLogoUrl(logoUrl);
        return tenantRepository.save(tenant);
    }

    public Optional<TenantSubscriptionResponse> getSubscriptionDetail(Long tenantId){
        return paymentServiceClientAdapter.getSubscriptionDetails(tenantId);
    }

    public Page<PaymentHistoryResponse> getTenantPaymentHistory(Long tenantId, Pageable pageable){
        return paymentServiceClientAdapter.getTenantPaymentHistory(tenantId, pageable);
    }

    @Cacheable(cacheNames = "public-tenant-storefront", key = "#tenantId")
    public TenantStorefrontInfo getPublicStorefront(Long tenantId){
        Tenant tenant = tenantRepository.findByIdWithDetails(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(NO_MERCHANT_DESCRIPTION, "404"));

        return new TenantStorefrontInfo(
                tenant.getId(),
                tenant.getName(),
                tenant.getBusinessName(),
                tenant.getLogoUrl(),
                tenant.getDescription(),
                tenant.getWebsiteUrl()
        );
    }

}
