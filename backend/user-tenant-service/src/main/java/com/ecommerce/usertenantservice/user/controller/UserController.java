package com.ecommerce.usertenantservice.user.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.usertenantservice.common.constants.ApiPaths;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentHistoryResponse;
import com.ecommerce.usertenantservice.tenant.service.UserTenantService;
import com.ecommerce.usertenantservice.user.constant.UserType;
import com.ecommerce.usertenantservice.user.controller.dto.request.KeycloakSyncRequest;
import com.ecommerce.usertenantservice.user.controller.dto.request.UserRequest;
import com.ecommerce.usertenantservice.user.controller.dto.response.UserResponse;
import com.ecommerce.usertenantservice.user.entity.User;
import com.ecommerce.usertenantservice.user.mapper.UserMapper;
import com.ecommerce.usertenantservice.user.service.ImageService;
import com.ecommerce.usertenantservice.user.service.UserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(ApiPaths.User.USER)
@Tag(name = "Users", description = "User profile and account management")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;
    private final ImageService imageService;
    private final UserTenantService userTenantService;

    public UserController(UserService userService, UserMapper userMapper, ImageService imageService, UserTenantService userTenantService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.imageService = imageService;
        this.userTenantService = userTenantService;
    }

    @Hidden
    @PreAuthorize("hasRole('sync_user')")
    @PostMapping("/")
    public ResponseEntity<UserResponse> syncUserRegistration(@RequestBody KeycloakSyncRequest  keycloakSyncRequest) {
        User user = userMapper.kcToEntity(keycloakSyncRequest);
        user.setUserType(UserType.USER);
        User savedUser = userService.syncUser(user);
        return ResponseEntity.ok(userMapper.toResponse(savedUser));
    }

    @Hidden
    @PreAuthorize("hasRole('sync_user')")
    @PutMapping("/{keycloakId}")
    public ResponseEntity<UserResponse> syncUserUpdate(@PathVariable UUID keycloakId, @RequestBody KeycloakSyncRequest keycloakSyncRequest) {
        log.info("keycloakSyncRequest: {}", keycloakSyncRequest);
        User user = userMapper.kcToEntity(keycloakSyncRequest);
        User updatedUser = userService.syncUserUpdate(user, keycloakId);
        return ResponseEntity.ok(userMapper.toResponse(updatedUser));
    }

    @Hidden
    @PreAuthorize("hasRole('sync_user')")
    @DeleteMapping("/{keycloakId}")
    public ResponseEntity<UserResponse> syncUserDelete(@PathVariable UUID keycloakId) {
        User deletedUser = userService.deleteUser(keycloakId);
        return ResponseEntity.ok(userMapper.toResponse(deletedUser));
    }

    @Operation(summary = "Update user profile", description = "Updates the authenticated user's own profile fields: name, phone, identity number.")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @PutMapping("/update")
    public ResponseEntity<UserResponse> updateUser(
            @RequestBody UserRequest userRequest,
            @CurrentUser AuthUser user) {
        User existingUser = userService.getExistingUser(user.keycloakId());
        log.info("OLD >> existingUser: {} {}", existingUser.getFirstName(), existingUser.getPhoneNumber());
        userMapper.updateUserFromRequest(userRequest, existingUser);
        log.info("NEW >> existingUser: {} {}", existingUser.getFirstName(), existingUser.getPhoneNumber());
        User updatedUser = userService.updateUser(existingUser);
        return ResponseEntity.ok(userMapper.toResponse(updatedUser));
    }

    // TODO [29.12.2025 06:48]: Bazi yerlerde optional var ama bazi yerlerde yok, duzeltilebilir...
    @Operation(summary = "Get current user", description = "Returns the authenticated user's full profile.")
    @ApiResponse(responseCode = "200", description = "User profile")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@CurrentUser AuthUser user) {
        User me =  userService.getExistingUser(user.keycloakId());
        boolean isMerchant = userTenantService.existsByUserIdAndIsActiveTrue(me.getId());
        UserResponse response = userMapper.toResponse(me);
        log.info("ME ENDPOINT DETECTED >> {}", user.email());
        if (isMerchant){
            UserResponse newResponse = response.withMerchantStatus(true);
            return ResponseEntity.ok(newResponse);
        }else{
            return ResponseEntity.ok(response);
        }
    }

    @Operation(summary = "Upload profile photo", description = "Uploads a profile photo to MinIO. Returns updated user with new photo URL. Max 5MB, JPEG/PNG/WebP.")
    @ApiResponse(responseCode = "200", description = "Photo uploaded")
    @PostMapping(value = "/upload-profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadProfileImage(
            @CurrentUser AuthUser user,
            @RequestParam("file") MultipartFile file) {

        // TODO [11.12.2025 18:54]: Eger bu imageService'i common jar yapacak olursak buradaki folderName:"profiles" kismini "products" vs gibi degistirip servisler arası ortak kullanip duplicate koddan kacinmis oluruz.
        String imageUrl = imageService.uploadImage(file, "profiles");

        // Entity mutasyonu + eski görsel temizliği servise ait (controller'da business logic yok)
        User updatedUser = userService.updateProfileImage(user.keycloakId(), imageUrl);

        return ResponseEntity.ok(userMapper.toResponse(updatedUser));
    }

    @Operation(summary = "Get my payment history", description = "Paginated list of all payment transactions made by the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Payment history page")
    @GetMapping("/payment-history")
    public ResponseEntity<Page<PaymentHistoryResponse>> getUserPaymentHistory(@CurrentUser AuthUser user, Pageable pageable){
        return ResponseEntity.ok(userService.getUserPaymentHistory(user.keycloakId(), pageable));
    }

}
