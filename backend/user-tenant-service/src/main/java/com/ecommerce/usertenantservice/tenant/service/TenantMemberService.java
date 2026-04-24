package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.usertenantservice.exception.MemberNotFoundException;
import com.ecommerce.usertenantservice.exception.OwnerTenantException;
import com.ecommerce.usertenantservice.tenant.constant.TenantRole;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantMemberResponse;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.entity.UserTenant;
import com.ecommerce.usertenantservice.tenant.mapper.TenantMapper;
import com.ecommerce.usertenantservice.user.entity.User;
import com.ecommerce.usertenantservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantMemberService {

    private final UserTenantService userTenantService;
    private final UserService userService;
    private final TenantProfileService tenantProfileService;
    private static final String NO_USER_DESCRIPTION = "Kullanıcı bulunamadı. Önce sisteme kayıt olmalı";
    private static final String ALREADY_MEMBER_DESCRIPTION = "Bu kullanıcı zaten ekibinizde";
    private final TenantMapper tenantMapper;
    private final AuthzCacheService authzCacheService;

    public Page<TenantMemberResponse> getTenantMembers(Long tenantId, Pageable pageable){
        return userTenantService.findByTenantId(tenantId, pageable).map(tenantMapper::toMemberResponse);
    }

    public List<UserTenant> getMyMemberships(UUID keycloakId) {
        return userTenantService.findAllMyMemberships(keycloakId);
    }

    @Transactional
    public void addMember(Long tenantId, String email, TenantRole role) {
        User user = userService.getByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(NO_USER_DESCRIPTION, "404"));

        boolean exists = userTenantService.existsByUserIdAndTenantId(user.getId(), tenantId);
        if (exists) {
            throw new IllegalStateException(ALREADY_MEMBER_DESCRIPTION);
        }

        Tenant tenant = tenantProfileService.getTenantById(tenantId);

        UserTenant membership = UserTenant.builder()
                .user(user)
                .tenant(tenant)
                .role(role)
                .isActive(true)
                .build();

        userTenantService.save(membership);
    }

    @Transactional
    public void updateMemberRole(Long tenantId, Long memberId, TenantRole newRole){
        UserTenant membership = userTenantService.findById(memberId)
                .filter(userTenant -> userTenant.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new MemberNotFoundException("Personel bulunamadı"));

        if(membership.getRole() == TenantRole.OWNER){
            throw new OwnerTenantException("Mağaza sahibinin rolü değiştirilemez");
        }

        if(newRole == TenantRole.OWNER){
            throw new OwnerTenantException("Sahiplik devri yapılamaz");
        }

        membership.setRole(newRole);
        userTenantService.save(membership);
        authzCacheService.evictUserCache(membership.getUser().getKeycloakId());
    }

    @Transactional
    public void removeMember(Long tenantId, Long memberId){
        UserTenant membership = userTenantService.findById(memberId)
                .filter(ut -> ut.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new MemberNotFoundException("Personel bulunamadı"));

        if (membership.getRole() == TenantRole.OWNER) {
            throw new OwnerTenantException("Mağaza sahibi kendini silemez");
        }

        membership.setIsActive(false);
        userTenantService.delete(membership);
        authzCacheService.evictUserCache(membership.getUser().getKeycloakId());
    }

}
