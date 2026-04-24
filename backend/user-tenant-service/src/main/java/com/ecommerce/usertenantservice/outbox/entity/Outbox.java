package com.ecommerce.usertenantservice.outbox.entity;

import com.ecommerce.common.entity.BaseOutbox;
import jakarta.persistence.*;
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