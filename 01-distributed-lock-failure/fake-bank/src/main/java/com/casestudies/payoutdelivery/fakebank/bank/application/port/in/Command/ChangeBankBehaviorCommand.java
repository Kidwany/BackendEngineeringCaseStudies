package com.casestudies.payoutdelivery.fakebank.bank.application.port.in.Command;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehaviorMode;

// An empty delay keeps the current one, so switching back to NORMAL doesn't lose the configured delay.
public record ChangeBankBehaviorCommand(BankBehaviorMode mode, Optional<Duration> delay) {

    public ChangeBankBehaviorCommand {
        Objects.requireNonNull(mode, "mode is required");
        Objects.requireNonNull(delay, "delay is required");
    }
}
