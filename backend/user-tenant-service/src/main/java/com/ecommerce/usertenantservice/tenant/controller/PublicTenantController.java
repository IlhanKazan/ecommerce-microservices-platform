package com.ecommerce.usertenantservice.tenant.controller;

import com.ecommerce.usertenantservice.common.constants.ApiPaths;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantStorefrontResponse;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.mapper.TenantMapper;
import com.ecommerce.usertenantservice.tenant.query.TenantStorefrontInfo;
import com.ecommerce.usertenantservice.tenant.service.TenantProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.Tenant.PUBLIC_TENANT)
@RequiredArgsConstructor
@Slf4j
public class PublicTenantController {

    private final TenantProfileService tenantProfileService;
    private final TenantMapper tenantMapper;

    @GetMapping("/{tenantId}/storefront")
    public ResponseEntity<TenantStorefrontResponse> getTenantStorefront(
            @PathVariable Long tenantId) {
        TenantStorefrontInfo info = tenantProfileService.getPublicStorefront(tenantId);
        return ResponseEntity.ok(tenantMapper.toStorefrontFromInfo(info));
    }

}
