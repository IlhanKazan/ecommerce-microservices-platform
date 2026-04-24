package com.ecommerce.usertenantservice.tenant.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.usertenantservice.tenant.constant.BusinessType;
import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;
import com.ecommerce.usertenantservice.user.entity.Address;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "tax_id")
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type")
    private BusinessType businessType;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(length = 50)
    private String iban;

    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    @Column(name = "legal_company_title")
    private String legalCompanyTitle;

    @Column(name = "iyzico_sub_merchant_key", length = 100, unique = true)
    private String iyzicoSubMerchantKey;

    @Builder.Default
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @SQLRestriction("is_active = true")
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private Set<UserTenant> members = new HashSet<>();

    @SQLRestriction("is_active = true")
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private Set<Address> addresses = new HashSet<>();

}
