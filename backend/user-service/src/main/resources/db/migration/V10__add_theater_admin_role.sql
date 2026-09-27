INSERT INTO "role" (id, created_at, updated_at, created_by, modified_by, role_name)
VALUES (nextval('my_entity_id_seq'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', 'ROLE_THEATER_ADMIN')
ON CONFLICT (role_name) DO NOTHING;
