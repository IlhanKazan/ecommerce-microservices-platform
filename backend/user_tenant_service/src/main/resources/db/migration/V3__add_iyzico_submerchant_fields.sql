ALTER TABLE tenants
    ADD COLUMN iban VARCHAR(50),
    ADD COLUMN tax_office VARCHAR(100),
    ADD COLUMN legal_company_title VARCHAR(255),
    ADD COLUMN iyzico_sub_merchant_key VARCHAR(100);

CREATE INDEX idx_tenants_sub_merchant_key ON tenants(iyzico_sub_merchant_key);

CREATE UNIQUE INDEX uk_tenants_iyzico_key ON tenants(iyzico_sub_merchant_key) WHERE iyzico_sub_merchant_key IS NOT NULL;