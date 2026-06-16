package com.ecommerce.usertenantservice.tenant.repository;

import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Platform admin için: opsiyonel status + isim/işletme adı filtreli sayfalı liste.
    // NOT: :q önceden hazırlanmış LIKE deseni ("%...%", lowercase). CONCAT/LOWER param üstünde
    // çalıştırılırsa null param Postgres'te bytea'ya bind olup "lower(bytea) does not exist" patlar.
    @Query("""
            SELECT t FROM Tenant t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:verified IS NULL OR t.isVerified = :verified)
              AND (:q IS NULL OR LOWER(t.name) LIKE :q OR LOWER(t.businessName) LIKE :q)
            """)
    Page<Tenant> searchForAdmin(@Param("status") TenantStatus status,
                                @Param("verified") Boolean verified,
                                @Param("q") String q,
                                Pageable pageable);
}
