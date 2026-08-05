CREATE TABLE account_summary (
    account_id   VARCHAR(64) PRIMARY KEY,
    owner_name   VARCHAR(200) NOT NULL,
    balance      NUMERIC(19,2) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    version      BIGINT NOT NULL
);

CREATE TABLE transaction_history (
    id            BIGSERIAL PRIMARY KEY,
    account_id    VARCHAR(64) NOT NULL,
    type          VARCHAR(20) NOT NULL,
    amount        NUMERIC(19,2) NOT NULL,
    reference     VARCHAR(200),
    balance_after NUMERIC(19,2) NOT NULL,
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transaction_history_account ON transaction_history (account_id);