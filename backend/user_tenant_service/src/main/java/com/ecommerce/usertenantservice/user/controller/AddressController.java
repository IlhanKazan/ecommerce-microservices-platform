package com.ecommerce.usertenantservice.user.controller;

import com.ecommerce.usertenantservice.common.constants.ApiPaths;
import com.ecommerce.usertenantservice.common.dto.AuthUser;
import com.ecommerce.usertenantservice.common.security.global.CurrentUser;
import com.ecommerce.usertenantservice.user.controller.dto.request.UserAddressRequest;
import com.ecommerce.usertenantservice.user.controller.dto.response.AddressResponse;
import com.ecommerce.usertenantservice.user.entity.Address;
import com.ecommerce.usertenantservice.user.mapper.AddressMapper;
import com.ecommerce.usertenantservice.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.User.ADDRESS)
@Tag(name = "Adres Yönetimi", description = "Kullanıcı adres ekleme, silme ve listeleme işlemleri")
public class AddressController {

    private final AddressService addressService;
    private final AddressMapper  addressMapper;

    public AddressController(AddressService addressService, AddressMapper addressMapper) {
        this.addressService = addressService;
        this.addressMapper = addressMapper;
    }

    @Operation(summary = "Get Addresses", description = "Mevcut kullanicin adreslerini getirir")
    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@CurrentUser AuthUser user) {
        List<Address> currentAddresses = addressService.getUserAddresses(user.keycloakId());
        return ResponseEntity.ok(addressMapper.addressListToAddressResponseList(currentAddresses));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createUserAddress(@CurrentUser AuthUser user, @RequestBody UserAddressRequest addressRequest) {
        Address newAddress = addressMapper.addressRequestToAddress(addressRequest);
        Address savedAddress = addressService.createUserAddress(user.keycloakId(), newAddress);
        return ResponseEntity.ok(addressMapper.addressToAddressResponse(savedAddress));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateUserAddress(@CurrentUser AuthUser user, @PathVariable Long id, @RequestBody UserAddressRequest addressRequest) {
        Address oldAddress = addressService.getUserAddress(user.keycloakId(), id);
        addressMapper.updateAddressFromRequest(addressRequest, oldAddress);
        Address savedAddress = addressService.updateUserAddress(oldAddress);
        return ResponseEntity.ok(addressMapper.addressToAddressResponse(savedAddress));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<List<AddressResponse>> setAsDefaultAddress(@CurrentUser AuthUser user, @PathVariable Long id) {
        try{
            addressService.setAsDefaultAddress(user.keycloakId(), id);
        }catch (Exception e){
            return ResponseEntity.badRequest().build();
        }
        List<Address> currentAddresses = addressService.getUserAddresses(user.keycloakId());
        return ResponseEntity.ok(addressMapper.addressListToAddressResponseList(currentAddresses));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteAddress(@CurrentUser AuthUser user, @PathVariable Long id){
        try{
            addressService.deleteAddress(user.keycloakId(), id);
            return ResponseEntity.ok(true);
        }catch (Exception e){
            return ResponseEntity.notFound().build();
        }
    }

}
