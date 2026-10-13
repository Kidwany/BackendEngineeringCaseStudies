package com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject;

import java.util.Objects;

public record PayoutId(String value) {

    public PayoutId {
        Objects.requireNonNull(value, "payout id is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("payout id must not be blank");
        }
    }

    public static PayoutId of(String value) {
        return new PayoutId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
