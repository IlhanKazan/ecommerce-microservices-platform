package com.ecommerce.paymentservice.inbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "inbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inbox {

    @Id
    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InboxStatus status;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    private String errorMessage;

    public enum InboxStatus { PENDING, PROCESSED, FAILED }

    @PrePersist
    protected void onCreate() {
        this.receivedAt = LocalDateTime.now();
    }
}
