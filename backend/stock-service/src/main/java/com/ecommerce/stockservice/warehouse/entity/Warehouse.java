package com.ecommerce.stockservice.warehouse.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse extends BaseEntity {

    private Long tenantId;
    private String code;
    private String name;
    private String locationDetails;

    @Builder.Default
    private Boolean isActive = true;
}