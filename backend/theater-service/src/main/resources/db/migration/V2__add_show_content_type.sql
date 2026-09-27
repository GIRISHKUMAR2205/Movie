ALTER TABLE scheduled_shows
    ADD COLUMN content_type VARCHAR(20) NOT NULL DEFAULT 'MOVIE';

ALTER TABLE scheduled_shows
    ADD CONSTRAINT scheduled_shows_content_type_check
    CHECK (content_type IN ('MOVIE', 'CONCERT'));
