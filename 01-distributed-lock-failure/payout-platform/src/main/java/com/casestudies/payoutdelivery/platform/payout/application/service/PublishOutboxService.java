package com.casestudies.payoutdelivery.platform.payout.application.service;

import java.time.Clock;

import com.casestudies.payoutdelivery.platform.payout.application.port.in.PublishOutboxUseCase;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxMessage;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxPublisherPort;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxRepository;

public class PublishOutboxService implements PublishOutboxUseCase {

    private final OutboxRepository outbox;
    private final OutboxPublisherPort publisher;
    private final Clock clock;
    private final int batchSize;

    public PublishOutboxService(OutboxRepository outbox, OutboxPublisherPort publisher, Clock clock, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batch size must be positive, was " + batchSize);
        }
        this.outbox = outbox;
        this.publisher = publisher;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Override
    public int publishPending() {
        int published = 0;
        for (OutboxMessage message : outbox.findPending(batchSize)) {
            publisher.publish(message);
            outbox.markPublished(message.eventId(), clock.instant());
            published++;
        }
        return published;
    }
}
