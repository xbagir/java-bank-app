ALTER TABLE notifications ADD COLUMN event_id UUID;
UPDATE notifications SET event_id = gen_random_uuid() WHERE event_id IS NULL;
ALTER TABLE notifications ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE notifications ADD CONSTRAINT uq_notifications_event_id UNIQUE (event_id);
