package com.casestudies.payoutdelivery.platform.payout.application.port.out;

import java.util.Objects;
import java.util.UUID;

public record OutboxMessage(UUID eventId, String aggregateId, String eventType, String payload) {

    public OutboxMessage {
        Objects.requireNonNull(eventId, "event id is required");
        Objects.requireNonNull(aggregateId, "aggregate id is required");
        Objects.requireNonNull(eventType, "event type is required");
        Objects.requireNonNull(payload, "payload is required");
    }
}
