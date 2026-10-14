package com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject;

import java.time.Duration;
import java.util.Objects;

public record BankBehavior(BankBehaviorMode mode, Duration delay) {

    public BankBehavior {
        Objects.requireNonNull(mode, "mode is required");
        Objects.requireNonNull(delay, "delay is required");
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative, was " + delay);
        }
    }

    public static BankBehavior normal(Duration delay) {
        return new BankBehavior(BankBehaviorMode.NORMAL, delay);
    }
}
