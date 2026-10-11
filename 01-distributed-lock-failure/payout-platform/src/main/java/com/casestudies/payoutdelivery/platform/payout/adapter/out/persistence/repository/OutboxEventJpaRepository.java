package com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.Entity.OutboxEventJpaEntity;
import com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.Entity.OutboxEventStatus;

interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    List<OutboxEventJpaEntity> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Limit limit);

    @Transactional
    @Modifying
    @Query("""
            UPDATE OutboxEventJpaEntity e
            SET e.status = PUBLISHED, e.publishedAt = :publishedAt
            WHERE e.id = :id AND e.status = PENDING""")
    int markPublished(@Param("id") UUID id, @Param("publishedAt") Instant publishedAt);
}
