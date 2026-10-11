package com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.Entity.OutboxEventStatus;
import com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.mapper.OutboxEventMapper;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxMessage;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxRepository;
import com.casestudies.payoutdelivery.platform.payout.domain.event.PayoutReady;

@Component
public class JpaOutboxRepositoryAdapter implements OutboxRepository {

    private final OutboxEventJpaRepository jpa;
    private final OutboxEventMapper mapper;

    JpaOutboxRepositoryAdapter(OutboxEventJpaRepository jpa, OutboxEventMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public void append(PayoutReady event) {
        jpa.save(mapper.toEntity(event));
    }

    @Override
    public List<OutboxMessage> findPending(int limit) {
        return jpa.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, Limit.of(limit)).stream()
                .map(mapper::toMessage)
                .toList();
    }

    @Override
    public void markPublished(UUID eventId, Instant publishedAt) {
        jpa.markPublished(eventId, publishedAt);
    }
}
