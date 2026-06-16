package com.ecommerce.usertenantservice.activity.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.usertenantservice.activity.constant.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_activity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 40)
    private ActivityType activityType;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "search_term", length = 255)
    private String searchTerm;
}
