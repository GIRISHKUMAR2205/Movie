ALTER TABLE outbox_event
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE,
    ADD COLUMN published_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_error TEXT;

CREATE INDEX ix_outbox_event_relay_pending
    ON outbox_event (status, next_attempt_at, created_at);
