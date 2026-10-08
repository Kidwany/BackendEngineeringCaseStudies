package com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.repository;

import com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.Entity.PayoutJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface PayoutJpaRepository extends JpaRepository<PayoutJpaEntity, String> {
}
