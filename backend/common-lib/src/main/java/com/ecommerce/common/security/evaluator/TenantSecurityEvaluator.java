package com.ecommerce.common.security.evaluator;

import com.ecommerce.common.security.feign.InternalAuthzClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;

@Component("tenantSecurity")
@RequiredArgsConstructor
@Slf4j
public class TenantSecurityEvaluator {

    private final StringRedisTemplate redisTemplate;
    private final InternalAuthzClient authzClient;

    /**
     * @param tenantId : URL'den gelen mağaza ID'si
     * @param roleName : Gerekli olan rol (OWNER, ADMIN vs.)
     */
    public boolean hasRole(Long tenantId, String roleName) {
        String userId = getCurrentUserKeycloakIdAsString();
        if (userId == null) return false;

        String redisKey = "authz:user:" + userId;
        String userRole = (String) redisTemplate.opsForHash().get(redisKey, "tenant_" + tenantId);

        if (userRole == null) {
            log.info("Cache Miss! Yetki Redis'te yok. UTS'ye soruluyor... User: {}, Tenant: {}", userId, tenantId);
            try {
                userRole = authzClient.getUserRoleInTenant(userId, tenantId);
            } catch (Exception e) {
                log.error("User-Tenant servisine ulaşılamadı veya yetki alınamadı!", e);
                return false;
            }
        }

        if (userRole == null || "NONE".equals(userRole)) return false;

        if ("OWNER".equals(userRole)) return true;

        return roleName.equals(userRole);
    }

    /**
     * Sadece "Bu dükkanın çalışanı mı?" diye bakar. Rol farketmez.
     * Detay ekranını görüntülemek için yeterlidir.
     */
    public boolean isMember(Long tenantId) {
        String userId = getCurrentUserKeycloakIdAsString();
        if (userId == null) {
            log.error("[IS_MEMBER] Token yok!");
            return false;
        }

        log.info("[IS_MEMBER] Kontrol Başladı | User: {} | Hedef Tenant: {}", userId, tenantId);

        String redisKey = "authz:user:" + userId;
        String userRole = (String) redisTemplate.opsForHash().get(redisKey, "tenant_" + tenantId);

        if (userRole == null) {
            log.info("Cache Miss! isMember için UTS'ye soruluyor...");
            try {
                userRole = authzClient.getUserRoleInTenant(userId, tenantId);
            } catch (Exception e) {
                log.error("UTS'ye ulaşılamadı", e);
                return false;
            }
        }

        boolean isMember = userRole != null && !"NONE".equals(userRole);

        if (isMember) {
            log.info("[IS_MEMBER] ÜYELİK DOĞRULANDI | Kullanıcı içeri girebilir.");
        } else {
            log.warn("[IS_MEMBER] YABANCI TESPİT EDİLDİ | Bu mağazanın üyesi değil.");
        }

        return isMember;
    }

    private String getCurrentUserKeycloakIdAsString() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) return null;
        return jwt.getClaimAsString("sub");
    }
}