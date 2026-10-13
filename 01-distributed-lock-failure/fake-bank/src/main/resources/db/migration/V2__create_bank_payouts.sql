-- No unique constraint on payout_id: on master every request is a separate financial payout,
-- so a duplicate request leaves a duplicate row.
CREATE TABLE bank_payouts (
    id          UUID           PRIMARY KEY,
    payout_id   VARCHAR(64)    NOT NULL,
    merchant_id VARCHAR(64)    NOT NULL,
    amount      NUMERIC(19, 4) NOT NULL,
    currency    CHAR(3)        NOT NULL,
    worker_id   VARCHAR(64)    NOT NULL,
    received_at TIMESTAMPTZ    NOT NULL,
    status      VARCHAR(32)    NOT NULL,

    CONSTRAINT bank_payouts_amount_positive CHECK (amount > 0),
    CONSTRAINT bank_payouts_status_known CHECK (status IN ('CREATED'))
);

CREATE INDEX bank_payouts_payout_id_idx ON bank_payouts (payout_id, received_at);
