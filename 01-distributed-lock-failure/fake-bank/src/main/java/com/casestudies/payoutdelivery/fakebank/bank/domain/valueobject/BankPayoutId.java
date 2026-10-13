package com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record BankPayoutId(UUID value) {

    public BankPayoutId {
        Objects.requireNonNull(value, "bank payout id is required");
    }

    public static BankPayoutId newId() {
        return new BankPayoutId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
