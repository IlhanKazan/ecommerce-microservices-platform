package com.ecommerce.usertenantservice.tenant.controller;

import com.ecommerce.usertenantservice.common.constants.ApiPaths;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantStorefrontResponse;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.mapper.TenantMapper;
import com.ecommerce.usertenantservice.tenant.query.TenantStorefrontInfo;
import com.ecommerce.usertenantservice.tenant.service.TenantProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Storefront", description = "Public storefront data — no authentication required")
public class PublicTenantController {

    private final TenantProfileService tenantProfileService;
    private final TenantMapper tenantMapper;

    @Operation(summary = "Get public storefront", description = "Returns publicly visible tenant info: store name, logo, description.", security = {})
    @ApiResponse(responseCode = "200", description = "Storefront data")
    @ApiResponse(responseCode = "404", description = "Tenant not found")
    @GetMapping("/{tenantId}/storefront")
    public ResponseEntity<TenantStorefrontResponse> getTenantStorefront(
            @PathVariable Long tenantId) {
        TenantStorefrontInfo info = tenantProfileService.getPublicStorefront(tenantId);
        return ResponseEntity.ok(tenantMapper.toStorefrontFromInfo(info));
    }

}
