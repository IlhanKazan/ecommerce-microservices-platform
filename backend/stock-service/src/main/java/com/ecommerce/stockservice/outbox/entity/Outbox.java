package com.ecommerce.stockservice.outbox.entity;

import com.ecommerce.common.entity.BaseOutbox;
import jakarta.persistence.*;
import lombok.*;
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