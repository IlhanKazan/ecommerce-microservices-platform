package com.ecommerce.productservice.client.adapter;

import com.ecommerce.productservice.client.UserTenantServiceClient;
import com.ecommerce.productservice.client.dto.TenantStorefrontResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserTenantClientAdapter {

    private final UserTenantServiceClient userTenantServiceClient;

    public TenantStorefrontResponse getStorefront(Long tenantId) {
        try {
            return userTenantServiceClient.getTenantStorefront(tenantId);
        } catch (FeignException e) {
            log.warn("UTS storefront alınamadı (tenantId={}): {}", tenantId, e.getMessage());
            return null;
        }
    }
}
