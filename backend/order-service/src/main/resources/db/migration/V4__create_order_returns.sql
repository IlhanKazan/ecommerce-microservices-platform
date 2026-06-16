-- ─── İade (return) sistemi ───────────────────────────────────────────────────

-- Yeni iade statüleri için orders CHECK constraint'ini güncelle (RETURN_REQUESTED/RETURNED/RETURN_REJECTED)
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_status;
ALTER TABLE orders ADD CONSTRAINT chk_orders_status CHECK (
    status IN ('CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED',
               'RETURN_REQUESTED', 'RETURNED', 'RETURN_REJECTED')
);

-- İade talepleri — yaşam döngüsü order'dan ayrı tutulur (sebep, çözen, not, iade tutarı)
CREATE TABLE order_returns
(
    id                BIGSERIAL      PRIMARY KEY,
    order_id          BIGINT         NOT NULL,
    user_id           UUID           NOT NULL,
    tenant_id         BIGINT         NOT NULL,
    reason            TEXT,
    status            VARCHAR(30)    NOT NULL,
    resolver_note     TEXT,
    resolved_by_admin BOOLEAN        NOT NULL DEFAULT FALSE,
    refund_amount     DECIMAL(12, 2),
    refund_succeeded  BOOLEAN,
    resolved_at       TIMESTAMP,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_order_returns_status CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED')),
    CONSTRAINT fk_order_returns_order   FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_order_returns_order  ON order_returns (order_id);
CREATE INDEX idx_order_returns_tenant ON order_returns (tenant_id);
CREATE INDEX idx_order_returns_status ON order_returns (status);

CREATE TRIGGER update_order_returns_modtime
    BEFORE UPDATE ON order_returns
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
