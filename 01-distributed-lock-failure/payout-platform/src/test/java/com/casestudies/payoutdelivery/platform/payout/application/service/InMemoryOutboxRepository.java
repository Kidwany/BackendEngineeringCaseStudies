package com.casestudies.payoutdelivery.platform.payout.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxMessage;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxRepository;
import com.casestudies.payoutdelivery.platform.payout.domain.event.PayoutReady;

// Keeps insertion order, which stands in for created_at ordering in the real table.
class InMemoryOutboxRepository implements OutboxRepository {

    private final List<PayoutReady> appended = new ArrayList<>();
    private final Map<UUID, OutboxMessage> pending = new LinkedHashMap<>();
    private final Map<UUID, Instant> published = new LinkedHashMap<>();

    @Override
    public void append(PayoutReady event) {
        appended.add(event);
        addPending(new OutboxMessage(event.eventId(), event.payoutId().value(), "PayoutReady", "{}"));
    }

    @Override
    public List<OutboxMessage> findPending(int limit) {
        return pending.values().stream().limit(limit).toList();
    }

    @Override
    public void markPublished(UUID eventId, Instant publishedAt) {
        if (pending.remove(eventId) != null) {
            published.put(eventId, publishedAt);
        }
    }

    void addPending(OutboxMessage message) {
        pending.put(message.eventId(), message);
    }

    List<PayoutReady> appended() {
        return appended;
    }

    List<UUID> pendingIds() {
        return List.copyOf(pending.keySet());
    }

    Optional<Instant> publishedAt(UUID eventId) {
        return Optional.ofNullable(published.get(eventId));
    }
}
