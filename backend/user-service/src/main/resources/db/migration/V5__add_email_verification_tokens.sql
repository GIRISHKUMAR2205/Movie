CREATE TABLE email_verification_tokens (
    id BIGINT PRIMARY KEY DEFAULT nextval('my_entity_id_seq'),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_email_verification_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX ix_email_verification_tokens_user ON email_verification_tokens (user_id);
