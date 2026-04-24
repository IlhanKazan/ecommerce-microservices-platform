-- Debezium CDC pattern'ı: processed ve sent_at entity'den kaldırıldı, DB'den de temizleniyor
ALTER TABLE outbox DROP COLUMN IF EXISTS processed;
ALTER TABLE outbox DROP COLUMN IF EXISTS sent_at;

-- BaseInbox uyumu: retry_count kolonu V1'de yoktu, ekleniyor
ALTER TABLE inbox ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;

-- Debezium için zorunlu: outbox tablosu DROP+CREATE sonrası sıfırlanır, her zaman set et
ALTER TABLE outbox REPLICA IDENTITY FULL;
