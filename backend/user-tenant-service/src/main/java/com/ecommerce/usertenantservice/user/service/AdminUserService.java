package com.ecommerce.usertenantservice.user.service;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.usertenantservice.user.controller.dto.response.AdminUserResponse;
import com.ecommerce.usertenantservice.user.entity.User;
import com.ecommerce.usertenantservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Platform admin kullanıcı yönetimi — listeleme + aktif/pasif (platform içi flag).
 * NOT: Pasifleştirme sadece UTS isActive flag'ini değiştirir; gerçek login-block
 * Keycloak Admin API gerektirir (TODO). Yetki controller'da (@PreAuthorize).
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(String q, Boolean active, Pageable pageable) {
        String like = StringUtils.hasText(q)
                ? "%" + q.trim().toLowerCase(Locale.of("tr")) + "%"
                : null;
        return userRepository.searchForAdmin(like, active, pageable).map(this::toResponse);
    }

    @Transactional
    public AdminUserResponse setActive(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + userId, "USER_NOT_FOUND"));
        user.setIsActive(active);
        return toResponse(userRepository.save(user));
    }

    private AdminUserResponse toResponse(User u) {
        return new AdminUserResponse(
                u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                u.getProfileImageUrl(), Boolean.TRUE.equals(u.getIsActive()), u.getCreatedAt());
    }
}
