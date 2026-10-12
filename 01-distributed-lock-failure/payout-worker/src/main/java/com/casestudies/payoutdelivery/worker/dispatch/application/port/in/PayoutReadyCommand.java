package com.casestudies.payoutdelivery.worker.dispatch.application.port.in;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.Money;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.PayoutId;

public record PayoutReadyCommand(UUID eventId, PayoutId payoutId, MerchantId merchantId, Money amount,
                                 Instant occurredAt) {

    public PayoutReadyCommand {
        Objects.requireNonNull(eventId, "event id is required");
        Objects.requireNonNull(payoutId, "payout id is required");
        Objects.requireNonNull(merchantId, "merchant id is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(occurredAt, "occurred at is required");
    }
}
