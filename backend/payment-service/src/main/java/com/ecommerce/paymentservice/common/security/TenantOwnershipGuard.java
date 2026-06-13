package com.ecommerce.paymentservice.common.security;

import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.paymentservice.client.UserTenantAuthzClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Mağaza sahipliği guard'ı — kart ve plan değiştirme gibi billing işlemlerinde
 * çağıran kullanıcının ilgili tenant'ın OWNER'ı olduğunu doğrular (IDOR koruması).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantOwnershipGuard {

    private static final String ROLE_OWNER = "OWNER";

    private final UserTenantAuthzClient authzClient;

    public void requireOwner(AuthUser user, Long tenantId) {
        if (user == null || user.keycloakId() == null) {
            throw new AccessDeniedException("Kimlik doğrulanamadı.");
        }

        String role;
        try {
            role = authzClient.getUserRole(user.keycloakId(), tenantId);
        } catch (Exception e) {
            log.error("Yetki sorgulaması başarısız — userId: {}, tenantId: {}: {}",
                    user.keycloakId(), tenantId, e.getMessage());
            throw new AccessDeniedException("Yetki doğrulanamadı.");
        }

        if (!ROLE_OWNER.equals(role)) {
            log.warn("Yetkisiz billing işlemi engellendi — userId: {}, tenantId: {}, role: {}",
                    user.keycloakId(), tenantId, role);
            throw new AccessDeniedException("Bu işlem için mağaza sahibi olmalısınız.");
        }
    }
}
