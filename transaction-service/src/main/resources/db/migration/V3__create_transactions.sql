CREATE TABLE IF NOT EXISTS transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_account_id  UUID           NOT NULL REFERENCES accounts(id),
    to_account_id    UUID           NOT NULL REFERENCES accounts(id),
    amount           NUMERIC(19, 4) NOT NULL,
    currency         VARCHAR(3)     NOT NULL DEFAULT 'USD',
    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDING_FRAUD_CHECK',
    idempotency_key  VARCHAR(255)   NOT NULL UNIQUE,
    description      TEXT,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_from_account ON transactions(from_account_id);
CREATE INDEX idx_transactions_to_account   ON transactions(to_account_id);
CREATE INDEX idx_transactions_status       ON transactions(status);
CREATE INDEX idx_transactions_idempotency  ON transactions(idempotency_key);
