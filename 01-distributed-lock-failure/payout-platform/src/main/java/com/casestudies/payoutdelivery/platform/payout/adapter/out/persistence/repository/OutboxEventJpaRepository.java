package com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.Entity.OutboxEventJpaEntity;

interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
}
