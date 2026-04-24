package com.ecommerce.usertenantservice.tenant.mapper;

import com.ecommerce.usertenantservice.tenant.controller.dto.request.CreateTenantRequest;
import com.ecommerce.usertenantservice.tenant.command.TenantCreationContext;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    TenantCreationContext toContext(CreateTenantRequest request);

    @Named("formatPhone")
    default String formatPhone(String phone) {
        if (phone == null) return "+905555555555";
        if (phone.startsWith("+")) return phone;
        if (phone.startsWith("0")) return "+9" + phone;
        return "+90" + phone;
    }
}