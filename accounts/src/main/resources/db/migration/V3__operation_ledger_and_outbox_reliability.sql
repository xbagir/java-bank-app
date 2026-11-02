CREATE TABLE account_operations (
    id BIGSERIAL PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    operation_type VARCHAR(20) NOT NULL,
    from_login VARCHAR(100) NOT NULL,
    to_login VARCHAR(100),
    amount NUMERIC(19, 2) NOT NULL,
    balance_before NUMERIC(19, 2),
    balance_after NUMERIC(19, 2),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_account_operations_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_account_operations_amount_scale CHECK (amount = round(amount, 2)),
    CONSTRAINT chk_account_operations_type CHECK (operation_type IN ('DEPOSIT', 'WITHDRAW', 'TRANSFER')),
    CONSTRAINT chk_account_operations_status CHECK (status IN ('PROCESSING', 'SUCCESS'))
);

CREATE INDEX idx_account_operations_created_at ON account_operations (created_at);

ALTER TABLE outbox_events ADD COLUMN event_id UUID;
UPDATE outbox_events SET event_id = gen_random_uuid() WHERE event_id IS NULL;
ALTER TABLE outbox_events ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE outbox_events ADD CONSTRAINT uq_outbox_events_event_id UNIQUE (event_id);
ALTER TABLE outbox_events ADD COLUMN locked_at TIMESTAMPTZ;
ALTER TABLE outbox_events ADD COLUMN last_error VARCHAR(1000);

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0);
ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_balance_scale CHECK (balance = round(balance, 2));
