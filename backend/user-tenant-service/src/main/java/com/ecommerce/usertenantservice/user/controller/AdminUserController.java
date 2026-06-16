package com.ecommerce.usertenantservice.user.controller;

import com.ecommerce.usertenantservice.common.constants.ApiPaths;
import com.ecommerce.usertenantservice.user.controller.dto.response.AdminUserResponse;
import com.ecommerce.usertenantservice.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.User.ADMIN_USER)
@RequiredArgsConstructor
@PreAuthorize("hasRole('platform-admin')")
@Tag(name = "Admin — Users", description = "Platform admin user management — list, enable/disable (requires platform-admin role)")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "List users", description = "Paginated platform users. Optional search (email/name) + active filter.")
    @ApiResponse(responseCode = "200", description = "User page")
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(adminUserService.listUsers(q, active, pageable));
    }

    @Operation(summary = "Enable/disable user", description = "Toggles the platform-level isActive flag. NOTE: does not block Keycloak login (real ban requires Keycloak Admin API).")
    @ApiResponse(responseCode = "200", description = "User status updated")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<AdminUserResponse> setStatus(
            @PathVariable Long userId,
            @RequestParam boolean active) {
        return ResponseEntity.ok(adminUserService.setActive(userId, active));
    }
}
