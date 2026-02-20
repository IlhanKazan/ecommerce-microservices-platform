package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.repository.TenantRepository;
import com.ecommerce.usertenantservice.user.entity.Address;
import com.ecommerce.usertenantservice.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantAddressService {

    private final AddressService addressService;
    private final TenantProfileService tenantProfileService;

    public Address getExistingAddress(Long tenantId){
        return addressService.getExistingTenantAddress(tenantId);
    }

    // Sadece bir adet magaza adresi kurali addressService'te kontrol ediliyor
    @Transactional
    public void addAddress(Long tenantId, Address address) {
        Tenant tenant = tenantProfileService.getTenantById(tenantId);
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

}
