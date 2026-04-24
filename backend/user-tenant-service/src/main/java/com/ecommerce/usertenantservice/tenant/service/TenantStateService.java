package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.usertenantservice.common.constants.AddressType;
import com.ecommerce.usertenantservice.tenant.constant.TenantRole;
import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;
import com.ecommerce.usertenantservice.tenant.domain.TenantCreationContext;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.entity.UserTenant;
import com.ecommerce.usertenantservice.tenant.repository.TenantRepository;
import com.ecommerce.usertenantservice.user.entity.Address;
import com.ecommerce.usertenantservice.user.entity.User;
import com.ecommerce.usertenantservice.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantStateService {

    private final TenantRepository tenantRepository;
    private final AddressService addressService;
    private final UserTenantService userTenantService;

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

        return tenant;
    }

    @Transactional
    public void markTenantAsPaymentFailed(Tenant tenant){
        tenant.setStatus(TenantStatus.PAYMENT_FAILED);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void activateTenant(Tenant tenant){
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void verifyTenant(Tenant tenant, String subMerchantKey){
        tenant.setIyzicoSubMerchantKey(subMerchantKey);
        tenant.setIsVerified(true);
        tenantRepository.save(tenant);
    }

}
