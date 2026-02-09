package com.ecommerce.usertenantservice.tenant.controller.dto.request;

import com.ecommerce.usertenantservice.tenant.constant.TenantRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull TenantRole newRole
) {
}
