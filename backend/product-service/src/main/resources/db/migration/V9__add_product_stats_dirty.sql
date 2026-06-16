-- Popülerlik senkronu: yalnızca sayacı DEĞİŞEN ürünleri ES'e taşımak için "dirty" işareti.
-- view_count/sale_count artınca true olur; scheduler yayınlayıp false'a çeker.
-- NOT: products tablosu Debezium publication'da değil (yalnızca outbox capture edilir) → CDC etkilenmez.
ALTER TABLE products ADD COLUMN stats_dirty BOOLEAN NOT NULL DEFAULT FALSE;

-- Mevcut sayaçlı ürünleri ilk senkron için bir kez işaretle (ES backfill).
UPDATE products SET stats_dirty = TRUE WHERE view_count > 0 OR sale_count > 0;

-- Scheduler sorgusu (WHERE stats_dirty = TRUE) için partial index.
CREATE INDEX idx_products_stats_dirty ON products (stats_dirty) WHERE stats_dirty = TRUE;
