package com.casestudies.payoutdelivery.worker.dispatch.application.port.out;

import java.time.Duration;

import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.PayoutId;

public class BankTimeoutException extends RuntimeException {

    public BankTimeoutException(PayoutId payoutId, Duration waited, Throwable cause) {
        super("No bank answer for payout " + payoutId + " within " + waited.toMillis()
                + "ms: the outcome is unknown, the bank may have paid.", cause);
    }
}
