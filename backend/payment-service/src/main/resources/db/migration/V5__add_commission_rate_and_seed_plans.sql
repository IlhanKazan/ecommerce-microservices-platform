-- commission_rate: subscription_plans'a ekle (her planın kendi komisyon oranı)
ALTER TABLE subscription_plans
    ADD COLUMN commission_rate DECIMAL(5,2) NOT NULL DEFAULT 8.00;

-- commission_rate: tenant_subscriptions'a snapshot olarak ekle
-- DEFAULT ile ekliyoruz; Postgres 11+ fast-path kullanır, satırları fiziksel güncellemez
-- → chk_next_billing constraint tetiklenmez
ALTER TABLE tenant_subscriptions
    ADD COLUMN commission_rate DECIMAL(5,2) DEFAULT 8.00;

-- Mevcut test planlarını temizle, mock planları ekle
TRUNCATE TABLE subscription_plans RESTART IDENTITY CASCADE;

INSERT INTO subscription_plans (name, price, currency, billing_cycle, commission_rate, features, is_active)
VALUES
    (
        'Başlangıç',
        0.00,
        'TRY',
        'MONTHLY',
        8.00,
        '{
          "maxProducts": 50,
          "teamMembers": 1,
          "support": "standard",
          "customDomain": false,
          "analytics": false,
          "apiAccess": false,
          "whiteLabel": false,
          "highlight": false
        }',
        true
    ),
    (
        'Büyüme',
        299.00,
        'TRY',
        'MONTHLY',
        5.00,
        '{
          "maxProducts": 500,
          "teamMembers": 3,
          "support": "priority",
          "customDomain": true,
          "analytics": true,
          "apiAccess": false,
          "whiteLabel": false,
          "highlight": true
        }',
        true
    ),
    (
        'İşletme',
        699.00,
        'TRY',
        'MONTHLY',
        3.00,
        '{
          "maxProducts": 2000,
          "teamMembers": 10,
          "support": "priority",
          "customDomain": true,
          "analytics": true,
          "apiAccess": true,
          "whiteLabel": false,
          "highlight": false
        }',
        true
    ),
    (
        'Kurumsal',
        1499.00,
        'TRY',
        'MONTHLY',
        1.00,
        '{
          "maxProducts": -1,
          "teamMembers": -1,
          "support": "dedicated",
          "customDomain": true,
          "analytics": true,
          "apiAccess": true,
          "whiteLabel": true,
          "highlight": false
        }',
        true
    );
