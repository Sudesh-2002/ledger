CREATE TABLE outbox (
    id              BIGSERIAL PRIMARY KEY,
    aggregate_id    VARCHAR(64) NOT NULL,
    sequence_number BIGINT NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    published       BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_unpublished ON outbox (published) WHERE published = false;