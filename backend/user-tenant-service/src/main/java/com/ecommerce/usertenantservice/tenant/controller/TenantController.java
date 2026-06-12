package com.ecommerce.usertenantservice.tenant.controller;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.usertenantservice.common.constants.ApiPaths;
import com.ecommerce.usertenantservice.tenant.controller.dto.request.*;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentHistoryResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantSubscriptionResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantSummaryResponse;
import com.ecommerce.usertenantservice.tenant.command.TenantCreationContext;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.entity.UserTenant;
import com.ecommerce.usertenantservice.tenant.mapper.PaymentMapper;
import com.ecommerce.usertenantservice.tenant.mapper.TenantMapper;
import com.ecommerce.usertenantservice.tenant.service.*;
import com.ecommerce.usertenantservice.user.controller.dto.request.UserAddressRequest;
import com.ecommerce.usertenantservice.user.entity.Address;
import com.ecommerce.usertenantservice.user.mapper.AddressMapper;
import com.ecommerce.usertenantservice.user.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@Tag(name = "Tenants", description = "Merchant store lifecycle — creation, payment, verification, member management, subscription")
public class TenantController {

    // TODO [10.02.2026 11:33]: CQRS araştırılacak, diğer mikroservisler için işe yarayabilir
    private final PaymentMapper paymentMapper;
    private final TenantMapper tenantMapper;
    private final AddressMapper addressMapper;
    private final ImageService imageService;
    private final TenantVerificationService tenantVerificationService;
    private final TenantAddressService tenantAddressService;
    private final TenantProfileService tenantProfileService;
    private final TenantMemberService tenantMemberService;
    private final TenantLifecycleService tenantLifecycleService;

    @Operation(summary = "Create tenant store", description = "Creates a new merchant store and immediately processes the subscription payment via iyzico. On success, tenant status becomes ACTIVE. Idempotent — use X-Idempotency-Key header.")
    @ApiResponse(responseCode = "200", description = "Tenant created and payment successful")
    @ApiResponse(responseCode = "402", description = "Payment rejected by iyzico")
    @ApiResponse(responseCode = "503", description = "Payment service unreachable — retry safe, tenant stays PENDING_PAYMENT")
    @PostMapping
    public ResponseEntity<Void> createTenant(
            @RequestBody @Valid CreateTenantRequest request,
            @CurrentUser AuthUser user) {

        TenantCreationContext context = paymentMapper.toContext(request);
        boolean result = tenantLifecycleService.createTenant(context, user.keycloakId());

        if (result) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Retry subscription payment", description = "Re-attempts payment for a tenant stuck in PAYMENT_FAILED or PENDING_PAYMENT status.")
    @ApiResponse(responseCode = "200", description = "Payment successful, tenant activated")
    @ApiResponse(responseCode = "402", description = "Payment rejected again")
    @PostMapping("/{tenantId}/retry-payment")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> retryPayment(
            @PathVariable Long tenantId,
            @RequestBody RetrySubscriptionRequest request,
            @CurrentUser AuthUser user){
        tenantLifecycleService.retryTenantPayment(tenantId, request.planId(), request.newCardInfo(), user.keycloakId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Submit business verification documents", description = "Uploads tax ID, trade registry and other verification data. Required before iyzico sub-merchant approval.")
    @ApiResponse(responseCode = "200", description = "Verification data saved")
    @PutMapping("/{tenantId}/verification")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> verifyTenant(
            @PathVariable Long tenantId,
            @RequestBody @Valid TenantVerificationRequest request,
            @CurrentUser AuthUser user){
        tenantVerificationService.verifyTenant(tenantId, request, user.keycloakId());
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "List my stores", description = "Returns all tenant stores owned by or membership of the authenticated user.")
    @ApiResponse(responseCode = "200", description = "List of tenant summaries")
    @GetMapping("/me")
    public ResponseEntity<List<TenantSummaryResponse>> getMyTenants(@CurrentUser AuthUser user) {

        List<UserTenant> memberships = tenantMemberService.getMyMemberships(user.keycloakId());
        List<TenantSummaryResponse> response = tenantMapper.userTenantToSummaryList(memberships);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get tenant detail", description = "Full tenant profile including status, address, members and subscription info.")
    @ApiResponse(responseCode = "200", description = "Tenant detail")
    @ApiResponse(responseCode = "403", description = "Not a member of this tenant")
    @ApiResponse(responseCode = "404", description = "Tenant not found")
    @GetMapping("/{tenantId}")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<TenantResponse> getTenantDetail(@PathVariable Long tenantId) {

        Tenant tenant = tenantProfileService.getTenantById(tenantId);
        TenantResponse response = tenantMapper.toDetail(tenant);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update general store info", description = "Updates non-critical fields: display name, description, website URL.")
    @ApiResponse(responseCode = "200", description = "Tenant updated")
    @PutMapping("/general/{tenantId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<TenantResponse> updateTenantGeneral(
            @RequestBody UpdateTenantGeneralRequest request,
            @PathVariable Long tenantId){
        Tenant tenant = tenantProfileService.getTenantById(tenantId);
        log.info("tenantGeneral Before >> {}", tenant.getBusinessName());
        tenantMapper.updateTenantGeneralFromRequest(request, tenant);
        log.info("tenantGeneral After >> {}", tenant.getBusinessName());
        tenantProfileService.save(tenant);
        Tenant saved = tenantProfileService.getTenantById(tenantId);
        TenantResponse response = tenantMapper.toDetail(saved);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update critical business info", description = "Updates legally significant fields: business name, tax ID, business type. May trigger re-verification.")
    @ApiResponse(responseCode = "200", description = "Tenant updated")
    @PutMapping("/critical/{tenantId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<TenantResponse> updateTenantCritical(
            @RequestBody UpdateTenantCriticalRequest request,
            @PathVariable Long tenantId,
            @CurrentUser AuthUser user){
        tenantVerificationService.updateTenantCritical(
                tenantId,
                request.taxId(),
                request.businessType(),
                request.legalCompanyTitle(),
                request.taxOffice(),
                request.iban(),
                user.keycloakId());
        Tenant saved = tenantProfileService.getTenantById(tenantId);
        TenantResponse response = tenantMapper.toDetail(saved);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Add tenant address")
    @ApiResponse(responseCode = "200", description = "Address added")
    @PostMapping("/{tenantId}/addresses")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> addAddress(
            @PathVariable Long tenantId,
            @RequestBody @Valid UserAddressRequest request) {

        Address addressEntity = addressMapper.addressRequestToAddress(request);
        tenantAddressService.addAddress(tenantId, addressEntity);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Update tenant address")
    @ApiResponse(responseCode = "200", description = "Address updated")
    @PutMapping("/{tenantId}/addresses/{addressId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> updateAddress(
            @PathVariable Long tenantId,
            @PathVariable Long addressId,
            @RequestBody @Valid UserAddressRequest request) {

        Address addressEntity = addressMapper.addressRequestToAddress(request);
        addressEntity.setId(addressId);

        tenantAddressService.updateAddress(tenantId, addressId, addressEntity);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete tenant address")
    @ApiResponse(responseCode = "200", description = "Address deleted")
    @DeleteMapping("/{tenantId}/addresses/{addressId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long tenantId,
            @PathVariable Long addressId) {

        tenantAddressService.removeAddress(tenantId, addressId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Upload tenant logo", description = "Uploads a logo image to MinIO. Returns updated tenant with new logo URL. Max 5MB, JPEG/PNG/WebP.")
    @ApiResponse(responseCode = "200", description = "Logo uploaded, tenant updated")
    @PostMapping(value = "/{tenantId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<TenantResponse> uploadLogo(
            @PathVariable Long tenantId,
            @RequestParam("file") MultipartFile file ){
        String imageUrl = imageService.uploadImage(file, "tenants");
        Tenant tenant = tenantProfileService.uploadLogo(tenantId, imageUrl);
        return ResponseEntity.ok(tenantMapper.toDetail(tenant));
    }

    // TODO [6.02.2026 14:50]: Uye ekleme islemi icin yeni bir davet tablosu olusturulacak ama v2 icin dusunulecek.
    @Operation(summary = "Add team member", description = "Adds an existing platform user to this tenant with a specified role.")
    @ApiResponse(responseCode = "200", description = "Member added")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PostMapping("/{tenantId}/members")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> addMember(@PathVariable Long tenantId, @RequestBody AddMemberRequest request) {
        tenantMemberService.addMember(tenantId, request.email(), request.role());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update member role")
    @ApiResponse(responseCode = "200", description = "Role updated")
    @PutMapping("/{tenantId}/members/{memberId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable Long tenantId,
            @PathVariable Long memberId,
            @RequestBody UpdateMemberRoleRequest request){
        tenantMemberService.updateMemberRole(tenantId, memberId, request.newRole());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove team member")
    @ApiResponse(responseCode = "200", description = "Member removed")
    @DeleteMapping("/{tenantId}/members/{memberId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long tenantId,
            @PathVariable Long memberId){
        tenantMemberService.removeMember(tenantId, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Pause store", description = "Temporarily pauses the store (ACTIVE → PASSIVE). Products are pulled from sale/search. Reversible via resume.")
    @ApiResponse(responseCode = "200", description = "Store paused")
    @ApiResponse(responseCode = "400", description = "Store is not ACTIVE")
    @PostMapping("/{tenantId}/pause")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> pauseTenant(@PathVariable Long tenantId) {
        tenantLifecycleService.pauseTenant(tenantId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Resume store", description = "Re-opens a paused store (PASSIVE → ACTIVE). Products return to sale/search.")
    @ApiResponse(responseCode = "200", description = "Store resumed")
    @ApiResponse(responseCode = "400", description = "Store is not PASSIVE")
    @PostMapping("/{tenantId}/resume")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> resumeTenant(@PathVariable Long tenantId) {
        tenantLifecycleService.resumeTenant(tenantId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Close store permanently", description = "Permanently closes the store (→ CLOSED, terminal). Products are pulled from sale and all members lose access.")
    @ApiResponse(responseCode = "200", description = "Store closed")
    @ApiResponse(responseCode = "400", description = "Store already closed")
    @PostMapping("/{tenantId}/close")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> closeTenant(@PathVariable Long tenantId) {
        tenantLifecycleService.closeTenant(tenantId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get active subscription", description = "Returns current subscription plan, billing cycle, next billing date and commission rate.")
    @ApiResponse(responseCode = "200", description = "Subscription detail")
    @GetMapping("/{tenantId}/subscription")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<TenantSubscriptionResponse> getSubscriptionDetail(@PathVariable Long tenantId){
        return tenantProfileService.getSubscriptionDetail(tenantId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Bu mağazaya ait abonelik bilgisi bulunamadı", "404"));
    }

    @Operation(summary = "Get tenant payment history", description = "Paginated list of all payment transactions for this tenant.")
    @ApiResponse(responseCode = "200", description = "Payment history page")
    @GetMapping("/{tenantId}/payment-details")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<Page<PaymentHistoryResponse>> getTenantPaymentHistory(@PathVariable Long tenantId, Pageable pageable){
        Page<PaymentHistoryResponse> response = tenantProfileService.getTenantPaymentHistory(tenantId, pageable);
        return ResponseEntity.ok(response);
    }

}