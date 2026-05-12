CREATE TABLE inbox (
    message_id    VARCHAR(255)  NOT NULL,
    event_type    VARCHAR(255)  NOT NULL,
    payload       JSONB         NOT NULL,
    status        VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    retry_count   INTEGER       NOT NULL DEFAULT 0,
    error_message TEXT,
    received_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    processed_at  TIMESTAMP,
    CONSTRAINT pk_inbox PRIMARY KEY (message_id)
);

CREATE INDEX idx_inbox_processed_at ON inbox (processed_at);

-- ─────────────────────────────────────────────────────────
-- mail_log: Her mail denemesinin audit kaydı.
-- inbox_message_id soft FK — inbox temizlenince log bozulmasın.
-- ─────────────────────────────────────────────────────────
CREATE TABLE mail_log (
    id                BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inbox_message_id  VARCHAR(255),
    event_type        VARCHAR(255),
    recipient_email   VARCHAR(320)  NOT NULL,
    subject           VARCHAR(500)  NOT NULL,
    template_name     VARCHAR(100)  NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    error_message     TEXT,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    sent_at           TIMESTAMP
);

CREATE INDEX idx_mail_log_recipient ON mail_log (recipient_email);
CREATE INDEX idx_mail_log_created_at ON mail_log (created_at);
CREATE INDEX idx_mail_log_inbox_message_id ON mail_log (inbox_message_id);
