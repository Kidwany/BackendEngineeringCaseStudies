package com.casestudies.payoutdelivery.platform.payout.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.casestudies.payoutdelivery.platform.payout.domain.event.PayoutReady;

public interface OutboxRepository {

    void append(PayoutReady event);

    List<OutboxMessage> findPending(int limit);

    void markPublished(UUID eventId, Instant publishedAt);
}
