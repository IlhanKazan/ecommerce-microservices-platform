-- Arama geçmişi: user_activity'ye SEARCH tipi için arama terimi kolonu.
-- (CDC'de değil → Debezium etkilenmez.)
ALTER TABLE user_activity ADD COLUMN search_term VARCHAR(255);
