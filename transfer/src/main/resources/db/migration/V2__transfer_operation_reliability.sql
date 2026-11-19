ALTER TABLE transfers ADD COLUMN request_id UUID;
ALTER TABLE transfers ADD COLUMN balance_after NUMERIC(19, 2);
ALTER TABLE transfers ADD COLUMN failure_reason VARCHAR(500);
CREATE UNIQUE INDEX uq_transfers_request_id
    ON transfers (request_id) WHERE request_id IS NOT NULL;

ALTER TABLE transfers
    ADD CONSTRAINT chk_transfers_amount_positive CHECK (amount > 0);
ALTER TABLE transfers
    ADD CONSTRAINT chk_transfers_amount_scale CHECK (amount = round(amount, 2));
ALTER TABLE transfers
    ADD CONSTRAINT chk_transfers_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'));

ALTER TABLE outbox_events ADD COLUMN event_id UUID;
UPDATE outbox_events SET event_id = gen_random_uuid() WHERE event_id IS NULL;
ALTER TABLE outbox_events ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE outbox_events ADD CONSTRAINT uq_transfer_outbox_events_event_id UNIQUE (event_id);
ALTER TABLE outbox_events ADD COLUMN locked_at TIMESTAMPTZ;
ALTER TABLE outbox_events ADD COLUMN last_error VARCHAR(1000);
