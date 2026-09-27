CREATE TABLE payment_outbox_events (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id),
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(2000)
);

CREATE INDEX ix_payment_outbox_pending ON payment_outbox_events (status, next_attempt_at, created_at);
