package com.ecommerce.usertenantservice.user.mapper;

import com.ecommerce.usertenantservice.user.controller.dto.request.UserAddressRequest;
import com.ecommerce.usertenantservice.user.controller.dto.response.AddressResponse;
import com.ecommerce.usertenantservice.user.entity.Address;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    Address addressRequestToAddress(UserAddressRequest addressRequest);

    AddressResponse addressToAddressResponse(Address address);

    List<AddressResponse> addressListToAddressResponseList(List<Address> addressList);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateAddressFromRequest(UserAddressRequest request, @MappingTarget Address entity);
}
