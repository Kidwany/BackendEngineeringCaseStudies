package com.casestudies.payoutdelivery.platform.payout.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.Money;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutStatus;

public record PayoutReady(UUID eventId, PayoutId payoutId, MerchantId merchantId, Money amount, Instant occurredAt) {

    public PayoutReady {
        Objects.requireNonNull(eventId, "event id is required");
        Objects.requireNonNull(payoutId, "payout id is required");
        Objects.requireNonNull(merchantId, "merchant id is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(occurredAt, "occurred at is required");
    }

    public static PayoutReady of(Payout payout, UUID eventId, Instant occurredAt) {
        if (payout.status() != PayoutStatus.READY) {
            throw new IllegalArgumentException("only a READY payout can be announced, " + payout.id() + " is " + payout.status());
        }
        return new PayoutReady(eventId, payout.id(), payout.merchantId(), payout.amount(), occurredAt);
    }
}
