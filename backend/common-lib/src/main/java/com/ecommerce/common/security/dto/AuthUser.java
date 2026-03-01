package com.ecommerce.common.security.dto;

import java.util.UUID;

public record AuthUser(
        UUID keycloakId,
        String username,
        String email,
        String firstName,
        String lastName
) {
}
