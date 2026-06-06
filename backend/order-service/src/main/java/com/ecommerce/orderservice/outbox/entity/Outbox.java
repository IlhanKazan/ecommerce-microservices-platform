package com.ecommerce.orderservice.outbox.entity;

import com.ecommerce.common.entity.BaseOutbox;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "outbox")
@SuperBuilder
@NoArgsConstructor
public class Outbox extends BaseOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
