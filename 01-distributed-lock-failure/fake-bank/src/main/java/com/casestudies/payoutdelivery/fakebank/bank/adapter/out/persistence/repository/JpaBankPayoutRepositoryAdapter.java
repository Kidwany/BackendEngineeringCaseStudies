package com.casestudies.payoutdelivery.fakebank.bank.adapter.out.persistence.repository;

import org.springframework.stereotype.Component;

import com.casestudies.payoutdelivery.fakebank.bank.adapter.out.persistence.mapper.BankPayoutPersistenceMapper;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.BankPayoutRepository;
import com.casestudies.payoutdelivery.fakebank.bank.domain.aggregate.BankPayout;

@Component
public class JpaBankPayoutRepositoryAdapter implements BankPayoutRepository {

    private final BankPayoutJpaRepository jpa;

    JpaBankPayoutRepositoryAdapter(BankPayoutJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(BankPayout payout) {
        jpa.save(BankPayoutPersistenceMapper.toEntity(payout));
    }
}
