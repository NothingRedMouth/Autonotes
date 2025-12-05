CREATE TABLE outbox_events
(
    id           BIGSERIAL PRIMARY KEY,
    aggregate_id BIGINT       NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_created_at ON outbox_events (created_at);
