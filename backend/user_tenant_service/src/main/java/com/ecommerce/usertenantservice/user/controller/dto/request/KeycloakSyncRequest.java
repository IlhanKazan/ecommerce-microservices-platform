package com.ecommerce.usertenantservice.user.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record KeycloakSyncRequest(
   @NotNull UUID keycloakId,
   String username,
   String email,
   String firstName,
   String lastName
) {}
