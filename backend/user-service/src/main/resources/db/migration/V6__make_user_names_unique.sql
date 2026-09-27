-- Usernames are accepted as login identifiers, so duplicates would make
-- authentication ambiguous. Email remains a supported login identifier too.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_user_name_lower
    ON users (LOWER(user_name));
