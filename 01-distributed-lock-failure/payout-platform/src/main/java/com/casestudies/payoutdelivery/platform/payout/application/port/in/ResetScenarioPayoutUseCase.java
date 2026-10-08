package com.casestudies.payoutdelivery.platform.payout.application.port.in;

/** Puts the scenario payout back to its exact initial state, whatever state it is in now. */
public interface ResetScenarioPayoutUseCase {

    void reset();
}
