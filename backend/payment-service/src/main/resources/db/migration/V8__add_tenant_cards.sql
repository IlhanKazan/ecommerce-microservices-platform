-- Kayıtlı kart vault'u (iyzico Card Storage aynası).
-- Kart numarası/CVC ASLA saklanmaz; sadece iyzico token + cüzdan anahtarı + son 4 hane + marka tutulur.
CREATE TABLE tenant_cards (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT,
    iyzico_card_token VARCHAR(255) NOT NULL,
    iyzico_card_user_key VARCHAR(255) NOT NULL,
    card_alias VARCHAR(100),
    last_four VARCHAR(4),
    card_association VARCHAR(30),
    card_family VARCHAR(50),
    bin_number VARCHAR(8),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_tenant_cards_tenant ON tenant_cards(tenant_id);

-- Tenant başına yalnızca bir varsayılan kart
CREATE UNIQUE INDEX uq_tenant_default_card ON tenant_cards(tenant_id) WHERE is_default = TRUE;

-- Renewal/upgrade tahsilatının kayıtlı kartla (token + cardUserKey) çalışması için cüzdan anahtarı
ALTER TABLE tenant_subscriptions ADD COLUMN iyzico_card_user_key VARCHAR(255);
