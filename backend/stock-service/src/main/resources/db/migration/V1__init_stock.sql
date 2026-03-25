CREATE OR REPLACE FUNCTION update_modified_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TABLE warehouses (
                            id BIGSERIAL PRIMARY KEY,
                            tenant_id BIGINT NOT NULL,
                            code VARCHAR(50) NOT NULL,
                            name VARCHAR(255) NOT NULL,
                            location_details TEXT,
                            is_active BOOLEAN DEFAULT TRUE,

                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT uq_tenant_warehouse_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_warehouses_tenant ON warehouses(tenant_id);

CREATE TRIGGER update_warehouses_modtime BEFORE UPDATE ON warehouses FOR EACH ROW EXECUTE PROCEDURE update_modified_column();

CREATE TABLE stocks (
                        id BIGSERIAL PRIMARY KEY,
                        tenant_id BIGINT NOT NULL,
                        warehouse_id BIGINT NOT NULL,
                        product_id BIGINT NOT NULL,
                        sku VARCHAR(100) NOT NULL,

                        available_quantity INTEGER NOT NULL DEFAULT 0,
                        reserved_quantity INTEGER NOT NULL DEFAULT 0,
                        low_stock_threshold INTEGER NOT NULL DEFAULT 5,

                        version BIGINT DEFAULT 0,

                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT chk_available_qty CHECK (available_quantity >= 0),
                        CONSTRAINT chk_reserved_qty CHECK (reserved_quantity >= 0),

                        CONSTRAINT uq_tenant_warehouse_product UNIQUE (tenant_id, warehouse_id, product_id),
                        CONSTRAINT fk_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE RESTRICT
);

CREATE INDEX idx_stocks_tenant_product ON stocks(tenant_id, product_id);
CREATE INDEX idx_stocks_sku ON stocks(sku);

CREATE TRIGGER update_stocks_modtime BEFORE UPDATE ON stocks FOR EACH ROW EXECUTE PROCEDURE update_modified_column();

CREATE TABLE stock_movements (
                                 id BIGSERIAL PRIMARY KEY,
                                 stock_id BIGINT NOT NULL,
                                 transaction_type VARCHAR(50) NOT NULL,
                                 reference_id VARCHAR(255),
                                 quantity_changed INTEGER NOT NULL,

                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_movement_stock FOREIGN KEY (stock_id) REFERENCES stocks (id) ON DELETE CASCADE
);

CREATE INDEX idx_stock_movements_stock ON stock_movements(stock_id);
CREATE INDEX idx_stock_movements_ref ON stock_movements(reference_id);

CREATE TABLE outbox (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        aggregate_type VARCHAR(100) NOT NULL,
                        aggregate_id VARCHAR(255) NOT NULL,
                        event_type VARCHAR(255) NOT NULL,
                        message_payload JSONB NOT NULL,

                        created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_created_at ON outbox(created_at);

CREATE TABLE inbox (
                       message_id VARCHAR(255) PRIMARY KEY,
                       event_type VARCHAR(255) NOT NULL,
                       payload JSONB NOT NULL,

                       processed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);