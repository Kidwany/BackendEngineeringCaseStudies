package com.casestudies.payoutdelivery.platform.payout.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxMessage;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxPublisherPort;

class PublishOutboxServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");

    private final InMemoryOutboxRepository outbox = new InMemoryOutboxRepository();
    private final RecordingPublisher publisher = new RecordingPublisher();

    @Test
    void publishesPendingEventsOldestFirstAndMarksEachOne() {
        OutboxMessage first = pending();
        OutboxMessage second = pending();

        int published = service(10).publishPending();

        assertThat(published).isEqualTo(2);
        assertThat(publisher.sent).containsExactly(first, second);
        assertThat(outbox.pendingIds()).isEmpty();
        assertThat(outbox.publishedAt(first.eventId())).contains(NOW);
        assertThat(outbox.publishedAt(second.eventId())).contains(NOW);
    }

    @Test
    void aPublishedEventIsNotSentAgain() {
        pending();
        PublishOutboxService service = service(10);
        service.publishPending();

        assertThat(service.publishPending()).isZero();
        assertThat(publisher.sent).hasSize(1);
    }

    @Test
    void aFailureKeepsThatEventAndLaterOnesPendingButKeepsEarlierMarks() {
        OutboxMessage first = pending();
        OutboxMessage failing = pending();
        OutboxMessage third = pending();
        publisher.failOn = failing.eventId();

        assertThatThrownBy(() -> service(10).publishPending()).hasMessage("broker unavailable");

        assertThat(publisher.sent).containsExactly(first);
        assertThat(outbox.publishedAt(first.eventId())).contains(NOW);
        assertThat(outbox.pendingIds()).containsExactly(failing.eventId(), third.eventId());
    }

    @Test
    void anEventThatFailedIsPublishedOnALaterPass() {
        OutboxMessage failing = pending();
        publisher.failOn = failing.eventId();
        PublishOutboxService service = service(10);
        assertThatThrownBy(service::publishPending).hasMessage("broker unavailable");

        publisher.failOn = null;

        assertThat(service.publishPending()).isEqualTo(1);
        assertThat(outbox.publishedAt(failing.eventId())).contains(NOW);
    }

    @Test
    void publishesAtMostOneBatchPerPass() {
        pending();
        pending();
        pending();

        assertThat(service(2).publishPending()).isEqualTo(2);
        assertThat(outbox.pendingIds()).hasSize(1);
    }

    @Test
    void rejectsANonPositiveBatchSize() {
        assertThatThrownBy(() -> service(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private PublishOutboxService service(int batchSize) {
        return new PublishOutboxService(outbox, publisher, Clock.fixed(NOW, ZoneOffset.UTC), batchSize);
    }

    private OutboxMessage pending() {
        UUID id = UUID.randomUUID();
        OutboxMessage message = new OutboxMessage(id, "PO-9001", "PayoutReady", "{\"eventId\":\"" + id + "\"}");
        outbox.addPending(message);
        return message;
    }

    private static final class RecordingPublisher implements OutboxPublisherPort {

        private final List<OutboxMessage> sent = new ArrayList<>();
        private UUID failOn;

        @Override
        public void publish(OutboxMessage message) {
            if (message.eventId().equals(failOn)) {
                throw new IllegalStateException("broker unavailable");
            }
            sent.add(message);
        }
    }
}
