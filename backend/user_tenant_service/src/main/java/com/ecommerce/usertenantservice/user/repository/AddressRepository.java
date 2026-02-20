package com.ecommerce.usertenantservice.user.repository;

import com.ecommerce.usertenantservice.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// TODO [7.12.2025 22:53]: Burada siparis verilirken isDefault true olan tek sonuc cagirilacak
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByUserKeycloakIdAndIsActiveTrue(@Param("keycloakId") UUID keycloakId);

    boolean existsByUserId(Long userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    void updateUserDefaultAddressToFalse(@Param("userId") Long userId);

    Address findByIdAndUserId(Long addressId, Long userId);

    Optional<Address> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsAddressByTenantIdAndIsActiveIsTrue(Long tenantId);

    Optional<Address> findByTenantIdAndIsActiveTrue(Long tenantId);
}
