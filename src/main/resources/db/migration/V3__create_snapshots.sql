CREATE TABLE account_snapshot (
    aggregate_id     VARCHAR(64) PRIMARY KEY,
    version           BIGINT NOT NULL,
    owner_name        VARCHAR(200) NOT NULL,
    balance           NUMERIC(19,2) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);