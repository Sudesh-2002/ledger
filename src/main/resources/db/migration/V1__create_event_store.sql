CREATE TABLE event_store (
    id              BIGSERIAL PRIMARY KEY,
    aggregate_id    VARCHAR(64)  NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    sequence_number BIGINT       NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_aggregate_sequence UNIQUE (aggregate_id, sequence_number)
);

CREATE INDEX idx_event_store_aggregate ON event_store (aggregate_id);