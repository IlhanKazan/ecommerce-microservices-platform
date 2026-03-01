package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.usertenantservice.tenant.repository.UserTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthzCacheService {

    private final UserTenantRepository userTenantRepository;
    private final StringRedisTemplate redisTemplate;

    public String fetchAndCacheUserRole(UUID keycloakId, Long tenantId) {
        var membershipOpt = userTenantRepository.findMemberRole(keycloakId, tenantId);

        String role = membershipOpt.map(userTenant -> userTenant.getRole().name()).orElse("NONE");

        String redisKey = "authz:user:" + keycloakId;
        redisTemplate.opsForHash().put(redisKey, "tenant_" + tenantId, role);
        redisTemplate.expire(redisKey, Duration.ofHours(2));

        log.info("Redis güncellendi. User: {}, Tenant: {}, Role: {}", keycloakId, tenantId, role);
        return role;
    }

    public void evictUserCache(UUID keycloakId) {
        String redisKey = "authz:user:" + keycloakId;
        redisTemplate.delete(redisKey);
        log.info("Kullanıcı yetkileri değişti, Redis cache temizlendi! User: {}", keycloakId);
    }
}