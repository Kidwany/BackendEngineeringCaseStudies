package com.casestudies.payoutdelivery.platform.payout.adapter.in.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.casestudies.payoutdelivery.platform.payout.application.port.in.PublishOutboxUseCase;

@Slf4j
@Component
@ConditionalOnBooleanProperty(name = "payout.outbox.publisher.enabled", matchIfMissing = true)
class OutboxPublishScheduler {

    private final PublishOutboxUseCase publishOutbox;

    OutboxPublishScheduler(PublishOutboxUseCase publishOutbox) {
        this.publishOutbox = publishOutbox;
    }

    @Scheduled(fixedDelayString = "${payout.outbox.publisher.poll-interval}")
    void publishPending() {
        try {
            int published = publishOutbox.publishPending();
            if (published > 0) {
                log.info("Published {} outbox event(s)", published);
            }
        } catch (RuntimeException e) {
            log.warn("Outbox publishing stopped; unpublished events stay PENDING for the next poll", e);
        }
    }
}
