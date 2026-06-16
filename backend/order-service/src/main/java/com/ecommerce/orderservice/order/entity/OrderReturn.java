package com.ecommerce.orderservice.order.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.orderservice.order.constant.ReturnStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_returns")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderReturn extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    // Yapılandırılmış sebep kodu (frontend seçeneği, ör. SIZE_MISMATCH)
    @Column(name = "reason_code", length = 40)
    private String reasonCode;

    // Serbest not (opsiyonel; "OTHER" sebebinde zorunlu)
    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnStatus status;

    @Column(name = "resolver_note", columnDefinition = "TEXT")
    private String resolverNote;

    @Column(name = "resolved_by_admin", nullable = false)
    private boolean resolvedByAdmin;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_succeeded")
    private Boolean refundSucceeded;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Builder
    public OrderReturn(Long orderId, UUID userId, Long tenantId, String reasonCode, String reason) {
        this.orderId = orderId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.reasonCode = reasonCode;
        this.reason = reason;
        this.status = ReturnStatus.REQUESTED;
    }

    public void approve(String note, boolean byAdmin, BigDecimal refundAmount, boolean refundSucceeded) {
        this.status = ReturnStatus.APPROVED;
        this.resolverNote = note;
        this.resolvedByAdmin = byAdmin;
        this.refundAmount = refundAmount;
        this.refundSucceeded = refundSucceeded;
        this.resolvedAt = LocalDateTime.now();
    }

    public void reject(String note, boolean byAdmin) {
        this.status = ReturnStatus.REJECTED;
        this.resolverNote = note;
        this.resolvedByAdmin = byAdmin;
        this.resolvedAt = LocalDateTime.now();
    }
}
