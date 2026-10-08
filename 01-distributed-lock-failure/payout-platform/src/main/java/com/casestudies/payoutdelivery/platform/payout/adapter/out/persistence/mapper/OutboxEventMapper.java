package com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.mapper;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.Entity.OutboxEventJpaEntity;
import com.casestudies.payoutdelivery.platform.payout.domain.event.PayoutReady;

import tools.jackson.databind.json.JsonMapper;

// The payload is the message contract the worker reads (Phase 10), so it is written from plain
// fields here rather than by serialising domain objects.
@Component
public class OutboxEventMapper {

    public static final String PAYOUT_READY = "PayoutReady";

    private final JsonMapper json;

    public OutboxEventMapper(JsonMapper json) {
        this.json = json;
    }

    public OutboxEventJpaEntity toEntity(PayoutReady event) {
        PayoutReadyPayload payload = new PayoutReadyPayload(
                event.eventId().toString(),
                event.payoutId().value(),
                event.merchantId().value(),
                event.amount().amount(),
                event.amount().currency().getCurrencyCode(),
                event.occurredAt());
        return OutboxEventJpaEntity.pending(
                event.eventId(), event.payoutId().value(), PAYOUT_READY, json.writeValueAsString(payload), event.occurredAt());
    }

    record PayoutReadyPayload(String eventId, String payoutId, String merchantId, BigDecimal amount, String currency,
                              Instant occurredAt) {
    }
}
