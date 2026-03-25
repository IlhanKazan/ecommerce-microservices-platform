package com.ecommerce.stockservice.warehouse.repository;

import com.ecommerce.stockservice.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByTenantIdAndId(Long tenantId, Long id);
    List<Warehouse> findAllByTenantId(Long tenantId);
}
