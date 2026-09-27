-- Do not alter V1 after it has been applied. This change must be forward-only.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
