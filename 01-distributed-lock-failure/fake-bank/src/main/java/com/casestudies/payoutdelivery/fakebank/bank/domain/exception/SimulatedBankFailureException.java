package com.casestudies.payoutdelivery.fakebank.bank.domain.exception;

public class SimulatedBankFailureException extends RuntimeException {

    public SimulatedBankFailureException() {
        super("Simulated bank failure: the payout was not processed.");
    }
}
