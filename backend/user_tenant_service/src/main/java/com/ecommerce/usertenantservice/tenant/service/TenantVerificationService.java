package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.usertenantservice.exception.ResourceNotFoundException;
import com.ecommerce.usertenantservice.tenant.constant.BusinessType;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantVerificationService {

    private final TenantRepository tenantRepository;
    private static final String NO_MERCHANT_DESCRIPTION = "Mağaza bulunamadı";

    // bu iş mantığı şu an platform verificationını askıya alıyor ve tekrar onay sürecine atıyor cunku kritik magaza verileri degisiyor
    @Transactional
    public void updateTenantCritical(Long tenantId, String newTaxId, BusinessType newBusinessType) {
        Tenant tenant = tenantRepository.findByIdWithDetails(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(NO_MERCHANT_DESCRIPTION));

        boolean changed = !tenant.getTaxId().equals(newTaxId) || !tenant.getBusinessType().equals(newBusinessType);

        if (changed) {
            tenant.setTaxId(newTaxId);
            tenant.setBusinessType(newBusinessType);
            // outboxa da commit atılacak ve gerekli servisler event dinleyecek
            tenant.setIsVerified(false);
        }

        tenantRepository.save(tenant);
    }

}
