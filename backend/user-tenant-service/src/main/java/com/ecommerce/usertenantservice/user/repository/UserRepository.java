package com.ecommerce.usertenantservice.user.repository;

import com.ecommerce.usertenantservice.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByKeycloakId(UUID keycloakId);
    Optional<User> findByEmail(String email);

    // Platform admin: opsiyonel arama (email/ad/soyad) + aktiflik filtreli sayfalı liste.
    // NOT: :q önceden hazırlanmış LIKE deseni ("%...%", lowercase) — null param bytea bug'ını önler.
    @Query("""
            SELECT u FROM User u
            WHERE (:q IS NULL OR LOWER(u.email) LIKE :q
                   OR LOWER(u.firstName) LIKE :q OR LOWER(u.lastName) LIKE :q)
              AND (:active IS NULL OR u.isActive = :active)
            """)
    Page<User> searchForAdmin(@Param("q") String q,
                              @Param("active") Boolean active,
                              Pageable pageable);
}
