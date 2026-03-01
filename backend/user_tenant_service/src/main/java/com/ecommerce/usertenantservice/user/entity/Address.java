package com.ecommerce.usertenantservice.user.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.usertenantservice.common.constants.AddressType;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type")
    private AddressType addressType;

    @Column(nullable = false)
    private String line1;

    private String line2;

    @Column(length = 100)
    private String city;

    @Column(name = "state_province", length = 100)
    private String stateProvince;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(length = 15)
    private String country;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Builder.Default
    @Column(name = "is_default")
    private Boolean isDefault = false;

    private String label;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Override
    protected void onCreate() {
        super.onCreate();
        validateOwnership();
    }

    @Override
    protected void onUpdate() {
        super.onUpdate();
        validateOwnership();
    }

    private void validateOwnership() {
        if (user != null && tenant != null) {
            throw new IllegalStateException("Bir adres aynı anda hem User'a hem Tenant'a ait olamaz.");
        }
        if (user == null && tenant == null) {
            throw new IllegalStateException("Adres sahipsiz olamaz (User veya Tenant zorunlu).");
        }
    }
}