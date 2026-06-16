package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.usertenantservice.common.constants.AddressType;
import com.ecommerce.usertenantservice.outbox.service.OutboxService;
import com.ecommerce.usertenantservice.tenant.constant.TenantRole;
import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;
import com.ecommerce.usertenantservice.tenant.command.TenantCreationContext;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.entity.UserTenant;
import com.ecommerce.usertenantservice.tenant.repository.TenantRepository;
import com.ecommerce.usertenantservice.user.entity.Address;
import com.ecommerce.usertenantservice.user.entity.User;
import com.ecommerce.usertenantservice.user.service.AddressService;
import com.ecommerce.usertenantservice.user.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantStateService {

    private final TenantRepository tenantRepository;
    private final AddressService addressService;
    private final UserTenantService userTenantService;
    private final OutboxService outboxService;
    private final ImageService imageService;

    @Transactional
    public Tenant saveInitialTenant(TenantCreationContext context, User user, Address addressToUse){

        Tenant tenant = new Tenant();
        tenant.setName(context.name());
        tenant.setBusinessName(context.businessName());
        tenant.setTaxId(context.taxId());
        tenant.setBusinessType(context.businessType());
        tenant.setContactEmail(context.contactEmail());
        tenant.setContactPhone(context.contactPhone());
        tenant.setDescription(context.description());
        tenant.setWebsiteUrl(context.websiteUrl());
        tenant.setStatus(TenantStatus.PENDING_PAYMENT);
        tenant = tenantRepository.save(tenant);

        Address tenantAddress = new Address();
        tenantAddress.setTenant(tenant);
        tenantAddress.setCity(addressToUse.getCity());
        tenantAddress.setCountry(addressToUse.getCountry());
        tenantAddress.setLine1(addressToUse.getLine1());
        tenantAddress.setZipCode(addressToUse.getZipCode());
        tenantAddress.setRecipientName(addressToUse.getRecipientName());
        tenantAddress.setAddressType(AddressType.BILLING);
        addressService.createTenantAddress(tenantAddress);

        UserTenant userTenant = UserTenant.builder()
                .user(user)
                .tenant(tenant)
                .role(TenantRole.OWNER)
                .isActive(true)
                .build();
        userTenantService.save(userTenant);

        outboxService.publishTenantCreatedEvent(tenant);
        return tenant;
    }

    @Transactional
    public void markTenantAsPaymentFailed(Tenant tenant){
        tenant.setStatus(TenantStatus.PAYMENT_FAILED);
        tenantRepository.save(tenant);
        outboxService.publishTenantPaymentFailedEvent(tenant);
    }

    @Transactional
    public void activateTenant(Tenant tenant){
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
        outboxService.publishTenantActivatedEvent(tenant);
    }

    @Transactional
    public void verifyTenant(Tenant tenant, String subMerchantKey){
        // iyzico'dan boş key geldiyse mağazayı doğrulanmış sayma — aksi halde key'siz "verified"
        // tenant ödeme alamaz ama UI doğrulanmış gösterir (sessiz bozulma).
        if (subMerchantKey == null || subMerchantKey.isBlank()) {
            throw new BusinessException(
                    "Mağaza doğrulanamadı: ödeme altyapısı anahtarı alınamadı. Lütfen tekrar deneyin.",
                    "SUBMERCHANT_KEY_MISSING");
        }
        tenant.setIyzicoSubMerchantKey(subMerchantKey);
        tenant.setIsVerified(true);
        tenantRepository.save(tenant);
    }

    /** Mağaza sahibi mağazasını geçici olarak duraklatır (ACTIVE → PASSIVE). Geri açılabilir. */
    @Transactional
    public void pauseTenant(Tenant tenant){
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new BusinessException("Sadece aktif bir mağaza duraklatılabilir.", "TENANT_NOT_ACTIVE");
        }
        tenant.setStatus(TenantStatus.PASSIVE);
        tenantRepository.save(tenant);
        outboxService.publishTenantStatusChangedEvent(tenant);
    }

    /** Duraklatılmış mağazayı yeniden açar (PASSIVE → ACTIVE). */
    @Transactional
    public void resumeTenant(Tenant tenant){
        if (tenant.getStatus() != TenantStatus.PASSIVE) {
            throw new BusinessException("Sadece duraklatılmış bir mağaza yeniden açılabilir.", "TENANT_NOT_PAUSED");
        }
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
        outboxService.publishTenantStatusChangedEvent(tenant);
    }

    /** Mağazayı kalıcı olarak kapatır (→ CLOSED). Terminal durum; geri dönüşü yoktur. */
    @Transactional
    public void closeTenant(Tenant tenant){
        if (tenant.getStatus() == TenantStatus.CLOSED) {
            throw new BusinessException("Mağaza zaten kapalı.", "TENANT_ALREADY_CLOSED");
        }
        String oldLogoUrl = tenant.getLogoUrl();
        tenant.setStatus(TenantStatus.CLOSED);
        tenantRepository.save(tenant);
        outboxService.publishTenantStatusChangedEvent(tenant);

        // Kapatma terminal; logo dosyasını MinIO'dan temizle (best-effort)
        imageService.deleteImage(oldLogoUrl);
    }

    /** Platform admin mağazayı askıya alır (ACTIVE/PASSIVE → SUSPENDED). Ürünler satıştan kalkar. Reaktive edilebilir. */
    @Transactional
    public void suspendTenant(Tenant tenant){
        TenantStatus status = tenant.getStatus();
        if (status == TenantStatus.CLOSED) {
            throw new BusinessException("Kapalı bir mağaza askıya alınamaz.", "TENANT_ALREADY_CLOSED");
        }
        if (status == TenantStatus.SUSPENDED) {
            throw new BusinessException("Mağaza zaten askıya alınmış.", "TENANT_ALREADY_SUSPENDED");
        }
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);
        outboxService.publishTenantStatusChangedEvent(tenant);
    }

    /** Platform admin askıya alınmış mağazayı yeniden aktive eder (SUSPENDED → ACTIVE). */
    @Transactional
    public void reactivateTenant(Tenant tenant){
        if (tenant.getStatus() != TenantStatus.SUSPENDED) {
            throw new BusinessException("Sadece askıya alınmış bir mağaza yeniden aktive edilebilir.", "TENANT_NOT_SUSPENDED");
        }
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
        outboxService.publishTenantStatusChangedEvent(tenant);
    }

}
