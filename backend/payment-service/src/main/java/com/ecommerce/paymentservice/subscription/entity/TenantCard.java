package com.ecommerce.paymentservice.subscription.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenant_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantCard extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "iyzico_card_token", nullable = false)
    private String iyzicoCardToken;

    @Column(name = "iyzico_card_user_key", nullable = false)
    private String iyzicoCardUserKey;

    @Column(name = "card_alias")
    private String cardAlias;

    @Column(name = "last_four")
    private String lastFour;

    @Column(name = "card_association")
    private String cardAssociation;

    @Column(name = "card_family")
    private String cardFamily;

    @Column(name = "bin_number")
    private String binNumber;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

}
