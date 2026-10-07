package com.casestudies.payoutdelivery.platform.payout.domain.valueobject;

public enum PayoutStatus {
    /** Prepared and announced; waiting for a worker to pick it up. */
    READY,
    /** A worker has claimed it and is sending it to the bank. */
    DISPATCHING,
    /** The bank accepted the payout. */
    DISPATCHED,
    /** The dispatch ended without the bank accepting it. */
    FAILED
}
