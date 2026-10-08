package com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.repository;

import org.springframework.stereotype.Component;

import com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.mapper.OutboxEventMapper;
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
}
