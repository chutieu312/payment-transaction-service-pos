CREATE TABLE IF NOT EXISTS accounts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(20)    NOT NULL UNIQUE,
    owner_id       UUID           NOT NULL REFERENCES users(id),
    balance        NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency       VARCHAR(3)     NOT NULL DEFAULT 'USD',
    status         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_owner_id ON accounts(owner_id);

-- Seed demo accounts
INSERT INTO accounts (account_number, owner_id, balance, currency, status)
SELECT 'ACC-ALICE-001', id, 50000.00, 'USD', 'ACTIVE' FROM users WHERE email = 'alice@example.com'
ON CONFLICT (account_number) DO NOTHING;

INSERT INTO accounts (account_number, owner_id, balance, currency, status)
SELECT 'ACC-BOB-001', id, 20000.00, 'USD', 'ACTIVE' FROM users WHERE email = 'bob@example.com'
ON CONFLICT (account_number) DO NOTHING;
