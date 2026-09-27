-- The service-level existence check improves the normal UX. This index is the
-- atomic concurrency guard when two requests arrive at the same time.
CREATE UNIQUE INDEX IF NOT EXISTS uq_role_requests_pending_user_role
    ON role_requests (user_id, requested_role_id)
    WHERE request_status = 'PENDING';
