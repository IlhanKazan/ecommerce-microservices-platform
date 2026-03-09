package com.ecommerce.productservice.inbox.entity;

import com.ecommerce.productservice.inbox.constant.InboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "inbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inbox {

    @Id
    @Column(name = "message_id", length = 255, nullable = false)
    private String messageId;

    @Column(nullable = false, length = 255)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private InboxStatus status = InboxStatus.PENDING;

    @Builder.Default
    private Integer retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime receivedAt;

    private LocalDateTime processedAt;
}