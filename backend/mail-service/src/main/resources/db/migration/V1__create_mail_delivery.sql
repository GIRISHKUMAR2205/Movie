CREATE TABLE mail_delivery (
    event_id UUID PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE
);
