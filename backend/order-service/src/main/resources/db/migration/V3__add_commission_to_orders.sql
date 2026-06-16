-- Platform komisyonu (payment-service'ten checkout SAGA üzerinden gelir).
-- Merchant net kazanç hesabı: net = total_amount - commission_amount.
ALTER TABLE orders ADD COLUMN commission_amount NUMERIC(12,2);
