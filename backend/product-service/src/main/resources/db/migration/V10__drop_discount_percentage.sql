-- Ölü kolon temizliği: products.discount_percentage hiç set edilmiyordu (hep 0).
-- İndirim modeli discounted_price üzerinden yürüyor. products CDC'de değil (yalnız outbox) → Debezium etkilenmez.
ALTER TABLE products DROP COLUMN IF EXISTS discount_percentage;
