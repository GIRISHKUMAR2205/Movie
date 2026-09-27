CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregatetype VARCHAR(255) NOT NULL,
    aggregateid VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_outbox_event_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX ix_outbox_event_created_at ON outbox_event (created_at);
