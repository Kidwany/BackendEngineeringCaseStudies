-- One row per payout: a single bank transfer to one merchant.
-- id is the business identifier (PO-9001); it also builds the bank idempotency key.
CREATE TABLE payouts (
    id          VARCHAR(64)    PRIMARY KEY,
    merchant_id VARCHAR(64)    NOT NULL,
    amount      NUMERIC(19, 4) NOT NULL,
    currency    CHAR(3)        NOT NULL,
    status      VARCHAR(16)    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT payouts_amount_positive CHECK (amount > 0),
    CONSTRAINT payouts_status_known CHECK (status IN ('READY', 'DISPATCHING', 'DISPATCHED', 'FAILED'))
);

CREATE INDEX payouts_merchant_id_idx ON payouts (merchant_id);
