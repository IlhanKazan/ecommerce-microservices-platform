package com.ecommerce.usertenantservice.tenant.controller.dto.request;

import com.ecommerce.usertenantservice.tenant.constant.TenantRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @Email @NotBlank String email,
        @NotNull TenantRole role
        ) {
}
