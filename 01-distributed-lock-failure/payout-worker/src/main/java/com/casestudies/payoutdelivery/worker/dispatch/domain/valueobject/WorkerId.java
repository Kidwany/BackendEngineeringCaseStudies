package com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject;

import java.util.Objects;

public record WorkerId(String value) {

    public WorkerId {
        Objects.requireNonNull(value, "worker id is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("worker id must not be blank");
        }
    }

    public static WorkerId of(String value) {
        return new WorkerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
