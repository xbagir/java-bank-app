ALTER TABLE cash_operations ADD COLUMN request_id UUID;
ALTER TABLE cash_operations ADD COLUMN balance_after NUMERIC(19, 2);
ALTER TABLE cash_operations ADD COLUMN failure_reason VARCHAR(500);
CREATE UNIQUE INDEX uq_cash_operations_request_id
    ON cash_operations (request_id) WHERE request_id IS NOT NULL;

ALTER TABLE cash_operations
    ADD CONSTRAINT chk_cash_operations_amount_positive CHECK (amount > 0);
ALTER TABLE cash_operations
    ADD CONSTRAINT chk_cash_operations_amount_scale CHECK (amount = round(amount, 2));
ALTER TABLE cash_operations
    ADD CONSTRAINT chk_cash_operations_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'));

ALTER TABLE outbox_events ADD COLUMN event_id UUID;
UPDATE outbox_events SET event_id = gen_random_uuid() WHERE event_id IS NULL;
ALTER TABLE outbox_events ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE outbox_events ADD CONSTRAINT uq_cash_outbox_events_event_id UNIQUE (event_id);
ALTER TABLE outbox_events ADD COLUMN locked_at TIMESTAMPTZ;
ALTER TABLE outbox_events ADD COLUMN last_error VARCHAR(1000);
