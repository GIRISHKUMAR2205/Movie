CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY DEFAULT nextval('my_entity_id_seq'),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    family_id UUID NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX ix_refresh_tokens_family_active ON refresh_tokens (family_id) WHERE revoked_at IS NULL;
CREATE INDEX ix_refresh_tokens_user_active ON refresh_tokens (user_id) WHERE revoked_at IS NULL;
