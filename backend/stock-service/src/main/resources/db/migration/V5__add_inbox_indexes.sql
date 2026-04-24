CREATE INDEX IF NOT EXISTS idx_inbox_status ON inbox(status);
CREATE INDEX IF NOT EXISTS idx_inbox_pending ON inbox(status) WHERE status = 'PENDING';