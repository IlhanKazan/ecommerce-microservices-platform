package com.ecommerce.usertenantservice.tenant.controller.dto.response;

import com.ecommerce.usertenantservice.tenant.constant.TenantRole;

import java.time.LocalDateTime;

public record TenantMemberResponse(
        Long memberId,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String profileImageUrl,
        TenantRole role,
        boolean isActive,
        LocalDateTime joinedAt
) {
}
