package com.casestudies.payoutdelivery.worker.dispatch.application.port.out;

import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.PayoutId;

public class BankRejectedException extends RuntimeException {

    public BankRejectedException(PayoutId payoutId, int status, String detail, Throwable cause) {
        super("Bank rejected payout " + payoutId + " with " + status + ": " + detail, cause);
    }
}
