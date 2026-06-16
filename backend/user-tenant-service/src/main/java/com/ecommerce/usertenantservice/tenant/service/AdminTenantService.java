package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantSummaryResponse;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.mapper.TenantMapper;
import com.ecommerce.usertenantservice.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Platform admin mağaza yönetimi — listeleme, detay, askıya alma/reaktive etme.
 * Yetki kontrolü controller'da (@PreAuthorize hasRole platform-admin).
 */
@Service
@RequiredArgsConstructor
public class AdminTenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final TenantProfileService tenantProfileService;
    private final TenantLifecycleService tenantLifecycleService;

    @Transactional(readOnly = true)
    public Page<TenantSummaryResponse> listStores(TenantStatus status, Boolean verified, String q, Pageable pageable) {
        // LIKE desenini Java'da kur ("%...%" + lowercase) — null param bytea bug'ını (lower(bytea)) önler.
        String like = StringUtils.hasText(q)
                ? "%" + q.trim().toLowerCase(Locale.of("tr")) + "%"
                : null;
        return tenantRepository.searchForAdmin(status, verified, like, pageable)
                .map(tenantMapper::tenantToSummary);
    }

    @Transactional(readOnly = true)
    public TenantResponse getStoreDetail(Long tenantId) {
        Tenant tenant = tenantProfileService.getTenantById(tenantId);
        return tenantMapper.toDetail(tenant);
    }

    public void suspend(Long tenantId) {
        tenantLifecycleService.suspendTenant(tenantId);
    }

    public void reactivate(Long tenantId) {
        tenantLifecycleService.reactivateTenant(tenantId);
    }
}
