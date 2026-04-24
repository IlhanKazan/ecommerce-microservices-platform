package com.ecommerce.paymentservice.outbox.entity;

import com.ecommerce.common.entity.BaseOutbox;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "outbox")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Outbox extends BaseOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
