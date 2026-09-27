-- Existing development databases created by Hibernate are baselined at V1.
-- These indexes make the same data-integrity guarantees available to them.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON users (email);
CREATE UNIQUE INDEX IF NOT EXISTS uq_role_name ON "role" (role_name);
CREATE UNIQUE INDEX IF NOT EXISTS uq_privilege_name ON privilege (privilege_name);
CREATE UNIQUE INDEX IF NOT EXISTS uq_oauth_accounts_provider_subject
    ON oauth_accounts (provider, provider_subject);

INSERT INTO "role" (id, created_at, updated_at, created_by, modified_by, role_name)
VALUES
    (nextval('my_entity_id_seq'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', 'ROLE_USER'),
    (nextval('my_entity_id_seq'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', 'ROLE_ADMIN'),
    (nextval('my_entity_id_seq'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', 'ROLE_SUPERADMIN')
ON CONFLICT (role_name) DO NOTHING;
