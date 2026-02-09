package com.example.payment_service.common.dto;

import java.util.UUID;

public record AuthUser(
        UUID keycloakId,
        String username,
        String email,
        String firstName,
        String lastName
) {
}
