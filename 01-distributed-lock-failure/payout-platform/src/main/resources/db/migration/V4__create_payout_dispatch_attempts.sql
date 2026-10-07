-- One row per worker attempt to send a payout to the bank. Several rows for one payout
-- is the normal shape here: it is the evidence of who held the lease and who called the bank.
CREATE TABLE payout_dispatch_attempts (
    id                      UUID         PRIMARY KEY,
    payout_id               VARCHAR(64)  NOT NULL REFERENCES payouts (id),
    worker_id               VARCHAR(64)  NOT NULL,
    lease_owner_id          VARCHAR(128),
    status                  VARCHAR(16)  NOT NULL,
    started_at              TIMESTAMPTZ  NOT NULL,
    lease_expires_at        TIMESTAMPTZ,
    validation_completed_at TIMESTAMPTZ,
    bank_request_sent_at    TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    failure_reason          TEXT,

    CONSTRAINT payout_dispatch_attempts_status_known CHECK (status IN (
        'STARTED', 'VALIDATED', 'PAUSED', 'SENT_TO_BANK', 'SUCCESS', 'FAILED'))
);

CREATE INDEX payout_dispatch_attempts_payout_id_idx ON payout_dispatch_attempts (payout_id, started_at);
