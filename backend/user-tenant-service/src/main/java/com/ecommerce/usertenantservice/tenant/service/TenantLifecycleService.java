package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.usertenantservice.exception.PaymentFailedException;
import com.ecommerce.usertenantservice.exception.PaymentServiceUnreachableException;
import com.ecommerce.usertenantservice.exception.TenantCreationException;
import com.ecommerce.usertenantservice.integration.payment.PaymentServiceClientAdapter;
import com.ecommerce.usertenantservice.integration.payment.dto.PaymentResult;
import com.ecommerce.usertenantservice.tenant.command.*;
import com.ecommerce.usertenantservice.tenant.constant.PaymentType;
import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
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
public class TenantLifecycleService {

    private final TenantStateService tenantStateService;
    private final AddressService addressService;
    private final UserService userService;
    private final PaymentServiceClientAdapter paymentServiceClientAdapter;
    private final TenantProfileService tenantProfileService;
    private final TenantAddressService tenantAddressService;
    private final AuthzCacheService authzCacheService;

    // TODO [10.02.2026 15:04]: Kullanıcının mevcutta ödemesi tamamlanamamış bir mağazası varsa o mağaza için abonelik ödemesi alınacak bir metoda ihtiyacımız var.
    // TODO [10.02.2026 11:21]: Idompotency dusunulecek!
    // manuel mapping hem performans icin hem de ic ice gecmis bir suru veri tasiyan nesne var diye tercih edildi
    public boolean createTenant(TenantCreationContext context, UUID keycloakId) {

        User user = userService.getExistingUser(keycloakId);

        Address addressToUse = formatAddress(keycloakId, context, user);

        Tenant tenant = tenantStateService.saveInitialTenant(context, user, addressToUse);

        PaymentProcessRequest paymentRequest = formatFeignRequest(context.cardInfo(), context.planId().longValue(), user, addressToUse, tenant);

        try {

            log.info("PAYMENT REQUEST >> {}", paymentRequest);

            PaymentResult paymentResult = paymentServiceClientAdapter.processPayment(paymentRequest);
            log.info("PAYMENT SERVICE /process RESPONSE >> {}", paymentResult);

            if (paymentResult.isInfrastructureError()) {
                // Ödeme servisi erişilemez veya timeout — ödemenin gerçekleşip gerçekleşmediği bilinmiyor.
                // Tenant PENDING_PAYMENT'ta kalır; kullanıcı retry-payment endpoint'i ile devam edebilir.
                log.error("Ödeme servisine ulaşılamadı. Ödeme durumu belirsiz. TenantId: {}", tenant.getId());
                throw new PaymentServiceUnreachableException(paymentResult.errorMessage());
            } else if (!paymentResult.isSuccess()) {
                log.warn("Ödeme başarısız (iş kuralı reddi)! Tenant FAILED statüsüne çekiliyor.");
                tenantStateService.markTenantAsPaymentFailed(tenant);
                throw new PaymentFailedException(paymentResult.errorMessage(), tenant.getId());
            }

        } catch (PaymentFailedException | PaymentServiceUnreachableException e) {
            throw e;
        } catch (Exception e) {
            // Feign iletişimi veya response parse sırasında beklenmedik hata — ödeme durumu bilinmiyor.
            // Tenant PENDING_PAYMENT'ta kalır; ghost payment riskini önler.
            log.error("Kritik hata: Ödeme servisi iletişiminde beklenmedik hata. TenantId: {}", tenant.getId(), e);
            throw new TenantCreationException("İşlem sırasında beklenmedik bir hata oluştu. Lütfen ödeme durumunuzu kontrol edin.");
        }

        // Try bloğundan exception fırlamadı — ödeme kesin başarılı. Aktivasyon başlıyor.
        log.info("Ödeme başarılı! Tenant ACTIVE statüsüne çekiliyor");
        tenantStateService.activateTenant(tenant);
        return true;
    }

    @Transactional
    public void retryTenantPayment(Long tenantId, Long planId, PaymentCardInfo newCardInfo, UUID keycloakId) {

        Tenant tenant = tenantProfileService.getTenantById(tenantId);
        if (tenant.getStatus().equals(TenantStatus.ACTIVE)) {
            throw new IllegalStateException("Bu mağaza zaten aktif.");
        }

        User user = userService.getExistingUser(keycloakId);

        Address billingAddress = tenantAddressService.getExistingAddress(tenantId);

        PaymentProcessRequest request = formatFeignRequest(newCardInfo, planId, user, billingAddress, tenant);

        log.info("RETRY PAYMENT BAŞLIYOR >> TenantId: {}", tenantId);
        PaymentResult paymentResult = paymentServiceClientAdapter.processPayment(request);

        if (paymentResult.isSuccess()) {
            log.info("Telafi ödemesi başarılı! Tenant ACTIVE yapılıyor.");
            tenantStateService.activateTenant(tenant);
        } else {
            log.warn("Telafi ödemesi YİNE başarısız.");
            throw new PaymentFailedException(paymentResult.errorMessage(), tenant.getId());
        }
    }

    /** Mağaza sahibi mağazasını duraklatır. Ürünleri satıştan kalkar (TENANT_STATUS_CHANGED event). */
    @Transactional
    public void pauseTenant(Long tenantId) {
        Tenant tenant = tenantProfileService.getTenantById(tenantId);
        tenantStateService.pauseTenant(tenant);
        tenantProfileService.evictStorefrontCache(tenantId);
        log.info("Mağaza duraklatıldı. TenantId: {}", tenantId);
    }

    /** Duraklatılmış mağazayı yeniden açar. Ürünleri tekrar satışa döner. */
    @Transactional
    public void resumeTenant(Long tenantId) {
        Tenant tenant = tenantProfileService.getTenantById(tenantId);
        tenantStateService.resumeTenant(tenant);
        tenantProfileService.evictStorefrontCache(tenantId);
        log.info("Mağaza yeniden açıldı. TenantId: {}", tenantId);
    }

    /** Platform admin mağazayı askıya alır. Ürünleri satıştan kalkar; üye erişimi korunur (close değil). */
    @Transactional
    public void suspendTenant(Long tenantId) {
        Tenant tenant = tenantProfileService.getTenantById(tenantId);
        tenantStateService.suspendTenant(tenant);
        tenantProfileService.evictStorefrontCache(tenantId);
        log.info("Mağaza askıya alındı (admin). TenantId: {}", tenantId);
    }

    /** Platform admin askıya alınmış mağazayı yeniden aktive eder. Ürünleri tekrar satışa döner. */
    @Transactional
    public void reactivateTenant(Long tenantId) {
        Tenant tenant = tenantProfileService.getTenantById(tenantId);
        tenantStateService.reactivateTenant(tenant);
        tenantProfileService.evictStorefrontCache(tenantId);
        log.info("Mağaza yeniden aktive edildi (admin). TenantId: {}", tenantId);
    }

    /** Mağazayı kalıcı kapatır. Tüm üyelerin authz cache'i temizlenir (rol artık NONE). */
    @Transactional
    public void closeTenant(Long tenantId) {
        Tenant tenant = tenantProfileService.getTenantById(tenantId);
        tenantStateService.closeTenant(tenant);
        tenant.getMembers().forEach(member ->
                authzCacheService.evictUserCache(member.getUser().getKeycloakId()));
        tenantProfileService.evictStorefrontCache(tenantId);
        log.info("Mağaza kalıcı olarak kapatıldı. TenantId: {}", tenantId);
    }

    public PaymentProcessRequest formatFeignRequest(PaymentCardInfo cardInfo, Long planId, User user, Address addressToUse, Tenant tenant){

        BuyerInfo buyerInfo = new BuyerInfo(
                user.getId().toString(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                formatPhone(user.getPhoneNumber()),
                tenant.getTaxId() != null ? tenant.getTaxId() : "11111111111",
                "127.0.0.1",
                addressToUse.getCity(),
                addressToUse.getCountry(),
                addressToUse.getZipCode(),
                addressToUse.getLine1()
        );

        AddressInfo addressInfo = new AddressInfo(
                addressToUse.getRecipientName(),
                addressToUse.getCity(),
                addressToUse.getCountry(),
                addressToUse.getLine1(),
                addressToUse.getZipCode()
        );

        return new PaymentProcessRequest(
                PaymentType.SUBSCRIPTION,
                planId,
                user.getId(),
                tenant.getId(),
                cardInfo,
                buyerInfo,
                addressInfo,
                addressInfo,
                tenant.getContactEmail() != null ? tenant.getContactEmail() : user.getEmail()
        );
    }

    public Address formatAddress(UUID keycloakId, TenantCreationContext context, User user){

        Address addressToUse;
        if (context.selectedAddressId() != null) {
            addressToUse = addressService.getUserAddress(keycloakId, context.selectedAddressId());
        } else {
            addressToUse = new Address();
            addressToUse.setCity(context.newAddress().city());
            addressToUse.setCountry(context.newAddress().country());
            addressToUse.setLine1(context.newAddress().fullAddress());
            addressToUse.setZipCode(context.newAddress().zipCode());
            addressToUse.setRecipientName(context.newAddress().contactName());
            addressToUse.setUser(user);
            addressToUse.setIsDefault(true);
            addressToUse = addressService.createTenantAddress(addressToUse);
        }

        return addressToUse;
    }

    private String formatPhone(String phone) {
        if (phone == null) return "+905555555555";
        if (phone.startsWith("+")) return phone;
        if (phone.startsWith("0")) return "+9" + phone;
        return "+90" + phone;
    }

}
