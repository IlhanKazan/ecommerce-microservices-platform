CREATE OR REPLACE FUNCTION update_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE subscription_plans (
                                    id BIGSERIAL PRIMARY KEY,
                                    name VARCHAR(100) NOT NULL,
                                    price DECIMAL(10,2) NOT NULL,
                                    currency VARCHAR(3) DEFAULT 'TRY',
                                    billing_cycle VARCHAR(20) NOT NULL,
                                    features JSONB,
                                    is_active BOOLEAN DEFAULT TRUE,
                                    created_at TIMESTAMP DEFAULT NOW(),
                                    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE tenant_subscriptions (
                                      id BIGSERIAL PRIMARY KEY,
                                      tenant_id BIGINT NOT NULL,
                                      plan_name VARCHAR(100),
                                      fee_amount DECIMAL(10,2),
                                      cycle_unit VARCHAR(20),
                                      next_billing_date DATE,
                                      iyzico_card_token VARCHAR(255),
                                      status VARCHAR(20),
                                      created_at TIMESTAMP DEFAULT NOW(),
                                      updated_at TIMESTAMP DEFAULT NOW(),
                                      started_at TIMESTAMP,
                                      canceled_at TIMESTAMP,
                                      last_successful_payment_date TIMESTAMP,
                                      failed_payment_count INTEGER DEFAULT 0,
                                      grace_period_end_date DATE,
                                      auto_renew BOOLEAN DEFAULT FALSE,
                                      cancellation_reason TEXT,

                                      CONSTRAINT chk_next_billing CHECK (next_billing_date >= CURRENT_DATE)
);

CREATE TRIGGER trg_tenant_subs_updated BEFORE UPDATE ON tenant_subscriptions
    FOR EACH ROW EXECUTE PROCEDURE update_timestamp();

CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,
                          order_id BIGINT,
                          subscription_id BIGINT,
                          tenant_id BIGINT,
                          customer_id BIGINT NOT NULL,
                          payment_type VARCHAR(50) NOT NULL,
                          amount DECIMAL(12,2) NOT NULL,
                          refunded_amount DECIMAL(12,2) DEFAULT 0,
                          net_amount DECIMAL(12,2),
                          payment_status VARCHAR(20) NOT NULL,
                          currency VARCHAR(3) DEFAULT 'TRY',
                          payment_method VARCHAR(50),
                          created_at TIMESTAMP DEFAULT NOW(),
                          updated_at TIMESTAMP DEFAULT NOW(),
                          paid_at TIMESTAMP,
                          failed_at TIMESTAMP,
                          failure_reason TEXT,
                          failure_code VARCHAR(50),
                          commission_amount DECIMAL(12,2),
                          commission_rate DECIMAL(5,2)
);

CREATE TRIGGER trg_payments_updated BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE PROCEDURE update_timestamp();

CREATE TABLE iyzico_transactions (
                                     id BIGSERIAL PRIMARY KEY,
                                     payment_id BIGINT,
                                     iyzico_txn_id VARCHAR(100),
                                     payment_card_hash VARCHAR(255),
                                     raw_request TEXT,
                                     raw_response TEXT,
                                     transaction_date TIMESTAMP DEFAULT NOW(),
                                     status VARCHAR(20),
                                     error_code VARCHAR(50),
                                     error_message TEXT,
                                     card_last_four VARCHAR(4),
                                     card_type VARCHAR(20),
                                     card_association VARCHAR(20),
                                     card_family VARCHAR(50),
                                     installment INTEGER,

                                     CONSTRAINT fk_iyzico_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

CREATE TABLE commission_rules (
                                  id BIGSERIAL PRIMARY KEY,
                                  tenant_id BIGINT,
                                  product_category_id INTEGER,
                                  commission_percentage DECIMAL(5,2) NOT NULL,
                                  min_amount DECIMAL(12,2),
                                  max_amount DECIMAL(12,2),
                                  valid_from DATE,
                                  valid_until DATE,
                                  priority INTEGER DEFAULT 0,
                                  rule_name VARCHAR(255)
);

CREATE TABLE outbox (
                        id BIGSERIAL PRIMARY KEY,
                        aggregate_type VARCHAR(50) NOT NULL,
                        aggregate_id VARCHAR(255) NOT NULL,
                        message_type VARCHAR(255) NOT NULL,
                        message_payload JSONB NOT NULL,
                        processed BOOLEAN DEFAULT FALSE,
                        created_at TIMESTAMP DEFAULT NOW(),
                        sent_at TIMESTAMP
);

CREATE TABLE inbox (
                       message_id VARCHAR(100) PRIMARY KEY,
                       event_type VARCHAR(100) NOT NULL,
                       payload JSONB,
                       status VARCHAR(20) NOT NULL,
                       received_at TIMESTAMP DEFAULT NOW(),
                       processed_at TIMESTAMP,
                       error_message TEXT
);

CREATE INDEX idx_inbox_status ON inbox(status);

CREATE INDEX idx_payments_tenant ON payments(tenant_id);
CREATE INDEX idx_payments_customer ON payments(customer_id);
CREATE INDEX idx_outbox_unprocessed ON outbox(created_at) WHERE processed = FALSE;
CREATE INDEX idx_tenant_subs_billing ON tenant_subscriptions(next_billing_date) WHERE status = 'ACTIVE';