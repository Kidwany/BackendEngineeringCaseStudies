-- Written in the same transaction as the payout change it announces; the publisher
-- sends PENDING rows to Kafka and marks them PUBLISHED.
CREATE TABLE outbox_events (
    id           UUID        PRIMARY KEY,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type   VARCHAR(64) NOT NULL,
    payload      JSONB       NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,

    CONSTRAINT outbox_events_status_known CHECK (status IN ('PENDING', 'PUBLISHED')),
    CONSTRAINT outbox_events_published_at_matches_status
        CHECK ((status = 'PUBLISHED') = (published_at IS NOT NULL))
);

-- The publisher's poll: oldest pending first. Partial, so published history doesn't bloat it.
CREATE INDEX outbox_events_pending_idx ON outbox_events (created_at) WHERE status = 'PENDING';

CREATE INDEX outbox_events_aggregate_id_idx ON outbox_events (aggregate_id, created_at);
