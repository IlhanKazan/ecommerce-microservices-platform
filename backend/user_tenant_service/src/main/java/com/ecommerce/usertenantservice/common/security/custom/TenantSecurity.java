package com.ecommerce.usertenantservice.common.security.custom;

import com.ecommerce.usertenantservice.tenant.constant.TenantRole;
import com.ecommerce.usertenantservice.tenant.entity.UserTenant;
import com.ecommerce.usertenantservice.tenant.repository.UserTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component("tenantSecurity")
@RequiredArgsConstructor
@Slf4j
public class TenantSecurity {

    private final UserTenantRepository userTenantRepository;

    /**
     * @param tenantId : URL'den gelen mağaza ID'si
     * @param roleName : Gerekli olan rol (OWNER, ADMIN vs.)
     */
    @Transactional(readOnly = true)
    public boolean hasRole(Long tenantId, String roleName) {


        UUID currentUserId = getCurrentUserKeycloakId();
        if (currentUserId == null) return false;

        var membershipOpt = userTenantRepository.findMemberRole(currentUserId, tenantId);

        if (membershipOpt.isEmpty()) {
            log.warn("Kullanıcı ({}) Tenant ({}) üyesi değil!", currentUserId, tenantId);
            return false;
        }

        UserTenant membership = membershipOpt.get();
        TenantRole currentRole = membership.getRole();

        if (currentRole == TenantRole.OWNER) return true;

        return currentRole.name().equals(roleName);
    }

    /**
     * Sadece "Bu dükkanın çalışanı mı?" diye bakar. Rol farketmez.
     * Detay ekranını görüntülemek için yeterlidir.
     */
    @Transactional(readOnly = true)
    public boolean isMember(Long tenantId) {
        UUID currentUserId = getCurrentUserKeycloakId();

        log.info("[IS_MEMBER] Kontrol Başladı | User: {} | Hedef Tenant: {}", currentUserId, tenantId);

        if (currentUserId == null) {
            log.error("[IS_MEMBER] Token yok!");
            return false;
        }

        boolean isMember = userTenantRepository.findMemberRole(currentUserId, tenantId).isPresent();

        if (isMember) {
            log.info("[IS_MEMBER] ÜYELİK DOĞRULANDI | Kullanıcı içeri girebilir.");
        } else {
            log.warn("[IS_MEMBER] YABANCI TESPİT EDİLDİ | Bu mağazanın üyesi değil.");
        }

        return isMember;
    }

    private UUID getCurrentUserKeycloakId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return null;
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getClaimAsString("sub"));
    }
}