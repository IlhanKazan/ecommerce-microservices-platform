-- ─── İade kuralları (FW-9.1) ─────────────────────────────────────────────────

-- Teslim tarihi: 14 günlük iade penceresinin hesaplanması için gerekli
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP;

-- Yapılandırılmış iade sebebi kodu (frontend seçeneği); reason kolonu serbest not olarak kalır
ALTER TABLE order_returns ADD COLUMN IF NOT EXISTS reason_code VARCHAR(40);
