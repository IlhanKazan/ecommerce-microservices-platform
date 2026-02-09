package com.ecommerce.usertenantservice.tenant.repository;

import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    // "Benim Keycloak ID'me sahip bir user'ın member olduğu tenantları getir"
    @Query("SELECT t FROM Tenant t " +
            "LEFT JOIN FETCH t.members m " +
            "LEFT JOIN FETCH m.user u " +
            "LEFT JOIN FETCH t.addresses a " +
            "WHERE t.id = :tenantId")
    Optional<Tenant> findByIdWithDetails(@Param("tenantId") Long tenantId);
}
