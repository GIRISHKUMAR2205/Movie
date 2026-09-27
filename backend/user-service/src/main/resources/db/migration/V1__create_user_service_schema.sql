CREATE SEQUENCE my_entity_id_seq START WITH 1 INCREMENT BY 100;

CREATE TABLE users (
    id BIGINT PRIMARY KEY DEFAULT nextval('my_entity_id_seq'),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    user_name VARCHAR(255) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password VARCHAR(255),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE "role" (
    id BIGINT PRIMARY KEY DEFAULT nextval('my_entity_id_seq'),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    role_name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_role_name UNIQUE (role_name)
);

CREATE TABLE privilege (
    id BIGINT PRIMARY KEY DEFAULT nextval('my_entity_id_seq'),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    privilege_name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_privilege_name UNIQUE (privilege_name)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES "role" (id)
);

CREATE TABLE role_privileges (
    role_id BIGINT NOT NULL,
    privilege_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, privilege_id),
    CONSTRAINT fk_role_privileges_role FOREIGN KEY (role_id) REFERENCES "role" (id),
    CONSTRAINT fk_role_privileges_privilege FOREIGN KEY (privilege_id) REFERENCES privilege (id)
);

CREATE TABLE oauth_accounts (
    id BIGINT PRIMARY KEY DEFAULT nextval('my_entity_id_seq'),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    provider VARCHAR(255) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT uq_oauth_accounts_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT fk_oauth_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE role_requests (
    id BIGINT PRIMARY KEY DEFAULT nextval('my_entity_id_seq'),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    user_id BIGINT NOT NULL,
    request_status VARCHAR(255) NOT NULL,
    role_status VARCHAR(255),
    requested_role_id BIGINT NOT NULL,
    CONSTRAINT fk_role_requests_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_role_requests_role FOREIGN KEY (requested_role_id) REFERENCES "role" (id)
);

CREATE TABLE role_audit (
    id BIGINT PRIMARY KEY DEFAULT nextval('my_entity_id_seq'),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    curr_role_id BIGINT NOT NULL,
    prev_request_status VARCHAR(255),
    curr_request_status VARCHAR(255) NOT NULL,
    role_request_id BIGINT NOT NULL,
    reason VARCHAR(500),
    CONSTRAINT fk_role_audit_role FOREIGN KEY (curr_role_id) REFERENCES "role" (id),
    CONSTRAINT fk_role_audit_request FOREIGN KEY (role_request_id) REFERENCES role_requests (id)
);
