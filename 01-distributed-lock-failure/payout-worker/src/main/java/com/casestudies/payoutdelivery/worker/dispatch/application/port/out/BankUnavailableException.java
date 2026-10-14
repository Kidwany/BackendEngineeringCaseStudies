package com.casestudies.payoutdelivery.worker.dispatch.application.port.out;

import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.PayoutId;

public class BankUnavailableException extends RuntimeException {

    public BankUnavailableException(PayoutId payoutId, String reason, Throwable cause) {
        super("Bank unavailable for payout " + payoutId + ": " + reason, cause);
    }
}
