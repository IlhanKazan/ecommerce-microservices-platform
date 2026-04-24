package com.ecommerce.usertenantservice.tenant.repository;

import com.ecommerce.usertenantservice.tenant.entity.UserTenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserTenantRepository extends JpaRepository<UserTenant, Long> {
    Optional<UserTenant> findByUserIdAndIsActiveTrue(Long userId);
    boolean existsByUserIdAndIsActiveTrue(Long id);

    @Query("SELECT ut FROM UserTenant ut " +
            "JOIN FETCH ut.tenant t " +
            "WHERE ut.user.keycloakId = :keycloakId " +
            "AND ut.isActive = true " +
            "AND t.status != 'CLOSED'")
    List<UserTenant> findAllMyMemberships(@Param("keycloakId") UUID keycloakId);

    @Query("SELECT ut FROM UserTenant ut " +
            "WHERE ut.user.keycloakId = :keycloakId " +
            "AND ut.tenant.id = :tenantId " +
            "AND ut.isActive = true")
    Optional<UserTenant> findMemberRole(
            @Param("keycloakId") UUID keycloakId,
            @Param("tenantId") Long tenantId
    );

    boolean existsByUserIdAndTenantId(Long userId, Long tenantId);

    Page<UserTenant> findByTenantId(Long tenantId, Pageable pageable);

}
