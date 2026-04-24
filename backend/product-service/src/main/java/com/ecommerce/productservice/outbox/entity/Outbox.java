package com.ecommerce.productservice.outbox.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false, length = 255)
    private String aggregateId;

    @Column(nullable = false, length = 255)
    private String messageType;

    // JSONB sütununu Java'da String olarak tutup JSON formatında kaydettiriyoruz
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String messagePayload;

    @Builder.Default
    @Column(nullable = false)
    private Boolean processed = false;

    @Builder.Default
    private Integer retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;
}