package com.casestudies.payoutdelivery.platform.payout.domain.valueobject;

import java.util.Objects;

public record MerchantId(String value) {

    public MerchantId {
        Objects.requireNonNull(value, "merchant id is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("merchant id must not be blank");
        }
    }

    public static MerchantId of(String value) {
        return new MerchantId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
