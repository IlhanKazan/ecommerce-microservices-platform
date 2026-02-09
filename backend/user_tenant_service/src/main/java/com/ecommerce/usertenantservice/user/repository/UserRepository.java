package com.ecommerce.usertenantservice.user.repository;

import com.ecommerce.usertenantservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByKeycloakId(UUID keycloakId);
    Optional<User> findByEmail(String email);
}
