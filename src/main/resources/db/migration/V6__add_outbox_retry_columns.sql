ALTER TABLE outbox ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE outbox ADD COLUMN dead_lettered BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_outbox_dead_letter ON outbox (dead_lettered) WHERE dead_lettered = true;