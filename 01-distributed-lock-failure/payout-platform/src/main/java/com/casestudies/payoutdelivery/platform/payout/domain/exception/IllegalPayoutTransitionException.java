package com.casestudies.payoutdelivery.platform.payout.domain.exception;

import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutStatus;

public class IllegalPayoutTransitionException extends RuntimeException {

    private final PayoutId payoutId;
    private final PayoutStatus from;
    private final PayoutStatus to;

    public IllegalPayoutTransitionException(PayoutId payoutId, PayoutStatus from, PayoutStatus to) {
        super("Payout " + payoutId + " cannot move from " + from + " to " + to);
        this.payoutId = payoutId;
        this.from = from;
        this.to = to;
    }

    public PayoutId payoutId() {
        return payoutId;
    }

    public PayoutStatus from() {
        return from;
    }

    public PayoutStatus to() {
        return to;
    }
}
