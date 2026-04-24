package com.ecommerce.usertenantservice.tenant.mapper;

import com.ecommerce.usertenantservice.tenant.controller.dto.request.UpdateTenantGeneralRequest;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.*;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.entity.UserTenant;
import com.ecommerce.usertenantservice.tenant.query.TenantStorefrontInfo;
import com.ecommerce.usertenantservice.user.entity.Address;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TenantMapper {

    @Mapping(source = "tenant.id", target = "id")
    @Mapping(source = "tenant.name", target = "name")
    @Mapping(source = "tenant.businessName", target = "businessName")
    @Mapping(source = "tenant.logoUrl", target = "logoUrl")
    @Mapping(source = "tenant.status", target = "status")
    @Mapping(source = "role", target = "myRole")
    TenantSummaryResponse userTenantToSummary(UserTenant userTenant);

    List<TenantSummaryResponse> userTenantToSummaryList(List<UserTenant> userTenants);


    @Mapping(source = "members", target = "members")
    @Mapping(source = "addresses", target = "addresses")
    TenantResponse toDetail(Tenant tenant);

    TenantSummaryResponse tenantToSummary(Tenant tenant);

    @Mapping(source = "id", target = "memberId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(source = "user.profileImageUrl", target = "profileImageUrl")
    @Mapping(source = "createdAt", target = "joinedAt")
    TenantMemberResponse toMemberResponse(UserTenant userTenant);

    @Mapping(source = "label", target = "label")
    @Mapping(source = "addressType", target = "type")
    TenantAddressResponse toAddressResponse(Address address);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTenantGeneralFromRequest(UpdateTenantGeneralRequest request, @MappingTarget Tenant tenant);

    TenantStorefrontResponse toStorefrontFromInfo(TenantStorefrontInfo info);
}