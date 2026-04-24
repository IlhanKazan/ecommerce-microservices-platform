package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.usertenantservice.exception.VerificationException;
import com.ecommerce.usertenantservice.integration.payment.PaymentServiceClientAdapter;
import com.ecommerce.usertenantservice.tenant.constant.BusinessType;
import com.ecommerce.usertenantservice.tenant.controller.dto.request.SubMerchantCreateRequest;
import com.ecommerce.usertenantservice.tenant.controller.dto.request.SubMerchantUpdateRequest;
import com.ecommerce.usertenantservice.tenant.controller.dto.request.TenantVerificationRequest;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.repository.TenantRepository;
import com.ecommerce.usertenantservice.user.entity.Address;
import com.ecommerce.usertenantservice.user.entity.User;
import com.ecommerce.usertenantservice.user.service.AddressService;
import com.ecommerce.usertenantservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantVerificationService {

    private final TenantRepository tenantRepository;
    private static final String NO_MERCHANT_DESCRIPTION = "Mağaza bulunamadı";
    private final TenantProfileService tenantProfileService;
    private final UserService userService;
    private final AddressService addressService;
    private final PaymentServiceClientAdapter paymentServiceClientAdapter;
    private final TenantStateService tenantStateService;

    // bu iş mantığı şu an platform verificationını askıya alıyor ve tekrar onay sürecine atıyor cunku kritik magaza verileri degisiyor
    @Transactional
    public void updateTenantCritical(Long tenantId, String newTaxId, BusinessType newBusinessType, String legalCompanyTitle, String taxOffice, String iban, UUID keycloakId) {

        Tenant tenant = tenantRepository.findByIdWithDetails(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(NO_MERCHANT_DESCRIPTION, "404"));

        boolean changed = !tenant.getTaxId().equals(newTaxId) ||
                !tenant.getBusinessType().equals(newBusinessType) ||
                !tenant.getIban().equals(iban) ||
                !tenant.getLegalCompanyTitle().equals(legalCompanyTitle) ||
                !tenant.getTaxOffice().equals(taxOffice);

        if (changed) {

            User owner = userService.getExistingUser(keycloakId);
            Address address = addressService.getExistingTenantAddress(tenantId);

            SubMerchantUpdateRequest updateRequest = new SubMerchantUpdateRequest(
                    tenant.getId(),
                    newBusinessType.name(),
                    iban,
                    taxOffice,
                    newTaxId,
                    legalCompanyTitle,
                    tenant.getContactEmail(),
                    tenant.getContactPhone(),
                    owner.getFirstName(),
                    owner.getLastName(),
                    address.getLine1() + " " + address.getCity() + "/" + address.getCountry(),
                    tenant.getIyzicoSubMerchantKey()
            );

            // Eğer Iyzico'da sorun olursa buradan Exception fırlayacak ve aşağıdaki DB güncellemeleri hiç yapılmayacak frontend'e hata dönecek.
            log.info("Iyzico SubMerchant güncelleme isteği: {}", tenantId);
            paymentServiceClientAdapter.updateSubMerchant(updateRequest);

            tenant.setTaxId(newTaxId);
            tenant.setBusinessType(newBusinessType);
            tenant.setLegalCompanyTitle(legalCompanyTitle);
            tenant.setTaxOffice(taxOffice);
            tenant.setIban(iban);
            tenant.setIsVerified(true);

            // Burada Outbox'a "TenantUpdatedEvent" atılabilir
        }else{
            return;
        }

        tenantRepository.save(tenant);
    }

    @Transactional
    public void verifyTenant(Long tenantId, TenantVerificationRequest request, UUID keycloakId){

        Tenant tenant = tenantProfileService.getTenantById(tenantId);

        if (Boolean.TRUE.equals(tenant.getIsVerified())){
            throw new VerificationException("Mağaza zaten onaylı");
        }

        tenant.setIban(request.iban());
        tenant.setLegalCompanyTitle(request.legalCompanyTitle());
        tenant.setTaxOffice(request.taxOffice());

        User owner = userService.getExistingUser(keycloakId);
        Address address = addressService.getExistingTenantAddress(tenantId);

        SubMerchantCreateRequest paymentSubRequest = new SubMerchantCreateRequest(
                tenant.getId(),
                tenant.getBusinessType().name(),
                tenant.getIban(),
                tenant.getTaxOffice(),
                tenant.getTaxId(),
                tenant.getLegalCompanyTitle(),
                tenant.getContactEmail(),
                tenant.getContactPhone(),
                owner.getFirstName(),
                owner.getLastName(),
                address.getLine1() + " " + address.getCity() + "/" + address.getCountry()
        );

        log.info("Iyzico SubMerchant oluşturma isteği: {}", tenantId);
        String subMerchantKey = paymentServiceClientAdapter.createSubMerchant(paymentSubRequest);

        tenantStateService.verifyTenant(tenant, subMerchantKey);

        log.info("Mağaza başarıyla doğrulandı ve SubMerchant oluşturuldu Key: {}", subMerchantKey);

    }

}
