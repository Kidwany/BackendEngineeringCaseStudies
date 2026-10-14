package com.casestudies.payoutdelivery.fakebank.bank.domain.exception;

import java.time.Duration;

public class SimulatedBankTimeoutException extends RuntimeException {

    public SimulatedBankTimeoutException(Duration waited) {
        super("Simulated bank timeout after " + waited.toMillis() + "ms: the payout was not processed.");
    }
}
