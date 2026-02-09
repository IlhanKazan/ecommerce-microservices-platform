package com.ecommerce.usertenantservice.tenant.controller;

import com.ecommerce.usertenantservice.common.constants.ApiPaths;
import com.ecommerce.usertenantservice.common.dto.AuthUser;
import com.ecommerce.usertenantservice.common.security.global.CurrentUser;
import com.ecommerce.usertenantservice.tenant.constant.TenantRole;
import com.ecommerce.usertenantservice.tenant.controller.dto.request.*;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantSummaryResponse;
import com.ecommerce.usertenantservice.tenant.domain.TenantCreationContext;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.entity.UserTenant;
import com.ecommerce.usertenantservice.tenant.mapper.PaymentMapper;
import com.ecommerce.usertenantservice.tenant.mapper.TenantMapper;
import com.ecommerce.usertenantservice.tenant.service.TenantService;
import com.ecommerce.usertenantservice.tenant.service.UserTenantService;
import com.ecommerce.usertenantservice.user.controller.dto.request.UserAddressRequest;
import com.ecommerce.usertenantservice.user.entity.Address;
import com.ecommerce.usertenantservice.user.mapper.AddressMapper;
import com.ecommerce.usertenantservice.user.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.Tenant.TENANT)
@RequiredArgsConstructor
@Slf4j
public class TenantController {

    private final TenantService tenantService;
    private final PaymentMapper paymentMapper;
    private final TenantMapper tenantMapper;
    private final AddressMapper addressMapper;
    private final ImageService imageService;

    @PostMapping
    public ResponseEntity<Void> createTenant(
            @RequestBody @Valid CreateTenantRequest request,
            @CurrentUser AuthUser user) {

        TenantCreationContext context = paymentMapper.toContext(request);
        boolean result = tenantService.createTenant(context, user.keycloakId());

        if (result) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<List<TenantSummaryResponse>> getMyTenants(@CurrentUser AuthUser user) {

        List<UserTenant> memberships = tenantService.getMyMemberships(user.keycloakId());
        List<TenantSummaryResponse> response = tenantMapper.userTenantToSummaryList(memberships);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<TenantResponse> getTenantDetail(@PathVariable Long tenantId) {

        Tenant tenant = tenantService.getTenantById(tenantId);
        TenantResponse response = tenantMapper.toDetail(tenant);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/general/{tenantId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<TenantResponse> updateTenantGeneral(
            @RequestBody UpdateTenantGeneralRequest request,
            @PathVariable Long tenantId){
        Tenant tenant = tenantService.getTenantById(tenantId);
        log.info("tenantGeneral Before >> {}", tenant.getBusinessName());
        tenantMapper.updateTenantGeneralFromRequest(request, tenant);
        log.info("tenantGeneral After >> {}", tenant.getBusinessName());
        tenantService.save(tenant);
        Tenant saved = tenantService.getTenantById(tenantId);
        TenantResponse response = tenantMapper.toDetail(saved);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/critical/{tenantId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<TenantResponse> updateTenantCritical(
            @RequestBody UpdateTenantCriticalRequest request,
            @PathVariable Long tenantId){
        tenantService.updateTenantCritical(tenantId, request.taxId(), request.businessType());
        Tenant saved = tenantService.getTenantById(tenantId);
        TenantResponse response = tenantMapper.toDetail(saved);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{tenantId}/addresses")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> addAddress(
            @PathVariable Long tenantId,
            @RequestBody @Valid UserAddressRequest request) {

        Address addressEntity = addressMapper.addressRequestToAddress(request);
        tenantService.addAddress(tenantId, addressEntity);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{tenantId}/addresses/{addressId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> updateAddress(
            @PathVariable Long tenantId,
            @PathVariable Long addressId,
            @RequestBody @Valid UserAddressRequest request) {

        Address addressEntity = addressMapper.addressRequestToAddress(request);
        addressEntity.setId(addressId);

        tenantService.updateAddress(tenantId, addressId, addressEntity);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{tenantId}/addresses/{addressId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long tenantId,
            @PathVariable Long addressId) {

        tenantService.removeAddress(tenantId, addressId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{tenantId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<TenantResponse> uploadLogo(
            @PathVariable Long tenantId,
            @RequestParam("file") MultipartFile file ){
        String imageUrl = imageService.uploadImage(file, "tenants");
        Tenant tenant = tenantService.uploadLogo(tenantId, imageUrl);
        return ResponseEntity.ok(tenantMapper.toDetail(tenant));
    }

    // TODO [6.02.2026 14:50]: Uye ekleme islemi icin yeni bir davet tablosu olusturulacak ama v2 icin dusunulecek.
    @PostMapping("/{tenantId}/members")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> addMember(@PathVariable Long tenantId, @RequestBody AddMemberRequest request) {
        tenantService.addMember(tenantId, request.email(), request.role());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{tenantId}/members/{memberId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable Long tenantId,
            @PathVariable Long memberId,
            @RequestBody UpdateMemberRoleRequest request){
        tenantService.updateMemberRole(tenantId, memberId, request.newRole());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{tenantId}/members/{memberId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long tenantId,
            @PathVariable Long memberId){
        tenantService.removeMember(tenantId, memberId);
        return ResponseEntity.ok().build();
    }

}