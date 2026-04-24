package com.ecommerce.usertenantservice.user.service;

import com.ecommerce.usertenantservice.user.entity.Address;
import com.ecommerce.usertenantservice.user.entity.User;
import com.ecommerce.usertenantservice.user.repository.AddressRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AddressService {
    
    private final AddressRepository addressRepository;
    private final UserService userService;
    private static final String ONLY_ONE_ADDRESS_EXP = "Magazanin sadece bir tane adresi olabilir.";

    public AddressService(AddressRepository addressRepository, UserService userService) {
        this.addressRepository = addressRepository;
        this.userService = userService;
    }

    public List<Address> getUserAddresses(UUID keycloakId) {
        return addressRepository.findAllByUserKeycloakIdAndIsActiveTrue(keycloakId);
    }

    public Address getUserAddress(UUID keycloakId, Long addressId) {
        User user =  userService.getExistingUser(keycloakId);
        return addressRepository.findByIdAndUserId(addressId, user.getId());
    }

    @Transactional
    public Address createUserAddress(UUID keycloakId, Address address) {
        User user = userService.getExistingUser(keycloakId);
        boolean hasAnyAddress = addressRepository.existsByUserId(user.getId());
        log.info("Recipient Name {}", address.getRecipientName());
        if (!hasAnyAddress) {
            address.setIsDefault(true);
        }
        else if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.updateUserDefaultAddressToFalse(user.getId());
        }

        address.setUser(user);
        return  addressRepository.save(address);
    }

    @Transactional
    public Address updateUserAddress(Address address) {
        try{
            addressRepository.save(address);
            return address;
        }catch (Exception e){
            log.error("Error while updating address {}", address, e);
            return null;
        }
    }

    @Transactional
    public void setAsDefaultAddress(UUID keycloakId, Long addressId) {
        User user = userService.getExistingUser(keycloakId);

        addressRepository.updateUserDefaultAddressToFalse(user.getId());

        Address address = addressRepository.findByIdAndUserId(addressId, user.getId());

        address.setIsDefault(true);
        addressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(UUID keycloakId, Long addressId) {
        User user = userService.getExistingUser(keycloakId);
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId());
        address.setIsActive(false);
        addressRepository.save(address);
    }

    @Transactional
    public Address createTenantAddress(Address address) {
        if(addressRepository.existsAddressByTenantIdAndIsActiveIsTrue(address.getTenant().getId())){
            throw new IllegalArgumentException(ONLY_ONE_ADDRESS_EXP);
        }
        return addressRepository.save(address);
    }

    public Address getTenantAddress(Long tenantId, Long addressId){
        return addressRepository.findByIdAndTenantId(addressId, tenantId)
                .orElseThrow(() -> new RuntimeException("Adres bulunamadı veya bu mağazaya ait değil."));
    }

    public Address getExistingTenantAddress(Long tenantId){
        return addressRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .orElseThrow(() -> new RuntimeException("Adres bulunamadı"));
    }

    @Transactional
    public void updateTenantAddress(Address address) {
        addressRepository.save(address);
    }

    @Transactional
    public void deleteTenantAddress(Address address){
        address.setIsActive(false);
        addressRepository.save(address);
    }

}
