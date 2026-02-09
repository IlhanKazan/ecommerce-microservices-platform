package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.usertenantservice.common.client.PaymentServiceClient;
import com.ecommerce.usertenantservice.common.constants.AddressType;
import com.ecommerce.usertenantservice.exception.MemberNotFoundException;
import com.ecommerce.usertenantservice.exception.OwnerTenantException;
import com.ecommerce.usertenantservice.exception.PaymentFailedException;
import com.ecommerce.usertenantservice.exception.ResourceNotFoundException;
import com.ecommerce.usertenantservice.tenant.constant.BusinessType;
import com.ecommerce.usertenantservice.tenant.constant.PaymentType;
import com.ecommerce.usertenantservice.tenant.constant.TenantRole;
import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantMemberResponse;
import com.ecommerce.usertenantservice.tenant.domain.*;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.entity.UserTenant;
import com.ecommerce.usertenantservice.tenant.mapper.TenantMapper;
import com.ecommerce.usertenantservice.tenant.repository.TenantRepository;
import com.ecommerce.usertenantservice.user.entity.Address;
import com.ecommerce.usertenantservice.user.entity.User;
import com.ecommerce.usertenantservice.user.service.AddressService;
import com.ecommerce.usertenantservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserTenantService userTenantService;
    private final AddressService addressService;
    private final UserService userService;
    private final PaymentServiceClient paymentServiceClient;
    private static final String NO_MERCHANT_DESCRIPTION = "Mağaza bulunamadı";
    private static final String NO_USER_DESCRIPTION = "Kullanıcı bulunamadı. Önce sisteme kayıt olmalı";
    private static final String ALREADY_MEMBER_DESCRIPTION = "Bu kullanıcı zaten ekibinizde";
    private final TenantMapper tenantMapper;

    // TODO [5.02.2026 21:57]: Feign yerine Kafkaya gecip outboxRepo.save diyerek event yayinlayip transaction kapatilabilir. Ardindan payment service'ten donen evente gore de tenant bilgileri vs onaylanir veya FAILED statusune cekilir.
    // TODO [5.02.2026 21:31]: Buradaki try catch yapilari duzenlenecek
    // manuel mapping hem performans icin hem de ic ice gecmis bir suru veri tasiyan nesne var diye tercih edildi
    @Transactional(rollbackFor = Exception.class)
    public boolean createTenant(TenantCreationContext context, UUID keycloakId) {

        User user = userService.getExistingUser(keycloakId);

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
            addressToUse = addressService.createTenantAddress(addressToUse);
        }


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

        tenant = tenantRepository.save(tenant); // ID olustu

        try {

            BuyerInfo buyerInfo = new BuyerInfo(
                    user.getId().toString(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    formatPhone(user.getPhoneNumber()),
                    context.taxId() != null ? context.taxId() : "11111111111",
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

            PaymentProcessRequest paymentRequest = new PaymentProcessRequest(
                    PaymentType.SUBSCRIPTION,
                    context.planId().longValue(),
                    user.getId(),
                    tenant.getId(),
                    context.cardInfo(),
                    buyerInfo,
                    addressInfo,
                    addressInfo
            );

            PaymentResponse response = paymentServiceClient.processPayment(paymentRequest);

            if (!response.success()) {
                throw new PaymentFailedException(response.message());
            }

            tenant.setStatus(TenantStatus.ACTIVE);
            tenantRepository.save(tenant);

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

            return true;

        } catch (Exception e) {
            // Buradaki exception sayesinde rollback atariz ve tenant olusamaz. Eger tenant olussun ama FAILED kalsin dersek farkli bir yapi kurmaliyiz
            throw new RuntimeException("Mağaza açılışında hata: " + e.getMessage());
        }
    }

    public List<UserTenant> getMyMemberships(UUID keycloakId) {
        return userTenantService.findAllMyMemberships(keycloakId);
    }

    public Tenant getTenantById(Long tenantId) {
        return tenantRepository.findByIdWithDetails(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(NO_MERCHANT_DESCRIPTION));
    }

    @Transactional
    public Tenant save(Tenant incomingData) {
        return tenantRepository.save(incomingData);
    }

    // bu iş mantığı şu an platform verificationını askıya alıyor ve tekrar onay sürecine atıyor cunku kritik magaza verileri degisiyor
    @Transactional
    public void updateTenantCritical(Long tenantId, String newTaxId, BusinessType newBusinessType) {
        Tenant tenant = tenantRepository.findByIdWithDetails(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(NO_MERCHANT_DESCRIPTION));

        boolean changed = !tenant.getTaxId().equals(newTaxId) || !tenant.getBusinessType().equals(newBusinessType);

        if (changed) {
            tenant.setTaxId(newTaxId);
            tenant.setBusinessType(newBusinessType);
            // outboxa da commit atılacak ve gerekli servisler event dinleyecek
            tenant.setIsVerified(false);
        }

        tenantRepository.save(tenant);
    }

    // Sadece bir adet magaza adresi kurali addressService'te kontrol ediliyor
    @Transactional
    public void addAddress(Long tenantId, Address address) {
        Tenant tenant = getTenantById(tenantId);
        address.setTenant(tenant);
        addressService.createTenantAddress(address);
    }

    @Transactional
    public void updateAddress(Long tenantId, Long addressId, Address incomingData) {
        Address existingAddress = addressService.getTenantAddress(tenantId, addressId);
        incomingData.setId(existingAddress.getId());
        incomingData.setTenant(existingAddress.getTenant());
        incomingData.setCreatedAt(existingAddress.getCreatedAt());
        addressService.updateTenantAddress(incomingData);
    }

    @Transactional
    public void removeAddress(Long tenantId, Long addressId) {
        Address address = addressService.getTenantAddress(tenantId, addressId);
        addressService.deleteTenantAddress(address);
    }

    @Transactional
    public Tenant uploadLogo(Long id, String logoUrl){
        Tenant tenant = getTenantById(id);
        tenant.setLogoUrl(logoUrl);
        return tenantRepository.save(tenant);
    }


    public Page<TenantMemberResponse> getTenantMembers(Long tenantId, Pageable pageable){
        return userTenantService.findByTenantId(tenantId, pageable).map(tenantMapper::toMemberResponse);
    }

    @Transactional
    public void addMember(Long tenantId, String email, TenantRole role) {
        User user = userService.getByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(NO_USER_DESCRIPTION));

        boolean exists = userTenantService.existsByUserIdAndTenantId(user.getId(), tenantId);
        if (exists) {
            throw new IllegalStateException(ALREADY_MEMBER_DESCRIPTION);
        }

        Tenant tenant = getTenantById(tenantId);

        UserTenant membership = UserTenant.builder()
                .user(user)
                .tenant(tenant)
                .role(role)
                .isActive(true)
                .build();

        userTenantService.save(membership);
    }

    @Transactional
    public void updateMemberRole(Long tenantId, Long memberId, TenantRole newRole){
        UserTenant membership = userTenantService.findById(memberId)
                .filter(userTenant -> userTenant.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new MemberNotFoundException("Personel bulunamadı"));

        if(membership.getRole() == TenantRole.OWNER){
            throw new OwnerTenantException("Mağaza sahibinin rolü değiştirilemez");
        }

        if(newRole == TenantRole.OWNER){
            throw new OwnerTenantException("Sahiplik devri yapılamaz");
        }

        membership.setRole(newRole);
        userTenantService.save(membership);
    }

    @Transactional
    public void removeMember(Long tenantId, Long memberId){
        UserTenant membership = userTenantService.findById(memberId)
                .filter(ut -> ut.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new MemberNotFoundException("Personel bulunamadı"));

        if (membership.getRole() == TenantRole.OWNER) {
            throw new OwnerTenantException("Mağaza sahibi kendini silemez");
        }

        membership.setIsActive(false);
        userTenantService.delete(membership);
    }

    private String formatPhone(String phone) {
        if (phone == null) return "+905555555555";
        if (phone.startsWith("+")) return phone;
        if (phone.startsWith("0")) return "+9" + phone;
        return "+90" + phone;
    }
}