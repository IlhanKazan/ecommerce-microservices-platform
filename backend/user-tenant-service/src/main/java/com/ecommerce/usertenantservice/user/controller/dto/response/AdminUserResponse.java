package com.ecommerce.usertenantservice.user.controller.dto.response;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String profileImageUrl,
        boolean isActive,
        LocalDateTime createdAt
) {}
