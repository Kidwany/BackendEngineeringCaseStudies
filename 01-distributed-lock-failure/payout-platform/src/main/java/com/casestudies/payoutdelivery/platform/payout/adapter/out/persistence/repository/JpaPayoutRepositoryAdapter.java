package com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.repository;

import java.util.Optional;

import com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.mapper.PayoutPersistenceMapper;
import org.springframework.stereotype.Component;

import com.casestudies.payoutdelivery.platform.payout.application.port.out.PayoutRepository;
import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;

@Component
public class JpaPayoutRepositoryAdapter implements PayoutRepository {

    private final PayoutJpaRepository jpa;

    JpaPayoutRepositoryAdapter(PayoutJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Payout> findById(PayoutId id) {
        return jpa.findById(id.value()).map(PayoutPersistenceMapper::toDomain);
    }

    @Override
    public void save(Payout payout) {
        jpa.save(PayoutPersistenceMapper.toEntity(payout));
    }
}
