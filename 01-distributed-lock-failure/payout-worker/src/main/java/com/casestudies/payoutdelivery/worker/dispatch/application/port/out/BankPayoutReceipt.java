package com.casestudies.payoutdelivery.worker.dispatch.application.port.out;

import java.util.Objects;
import java.util.UUID;

public record BankPayoutReceipt(UUID bankPayoutId) {

    public BankPayoutReceipt {
        Objects.requireNonNull(bankPayoutId, "bank payout id is required");
    }
}
