CREATE TABLE idempotency_key (
    idempotency_key   VARCHAR(100) PRIMARY KEY,
    request_path      VARCHAR(200) NOT NULL,
    status             VARCHAR(20) NOT NULL,   -- PROCESSING | COMPLETED
    response_status    INT,
    response_body      TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);