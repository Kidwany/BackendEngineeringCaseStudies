package com.casestudies.payoutdelivery.fakebank.bank.adapter.out.persistence.mapper;

import com.casestudies.payoutdelivery.fakebank.bank.adapter.out.persistence.entity.BankPayoutJpaEntity;
import com.casestudies.payoutdelivery.fakebank.bank.domain.aggregate.BankPayout;

public final class BankPayoutPersistenceMapper {

    private BankPayoutPersistenceMapper() {
    }

    public static BankPayoutJpaEntity toEntity(BankPayout payout) {
        return new BankPayoutJpaEntity(
                payout.id().value(),
                payout.payoutId().value(),
                payout.merchantId().value(),
                payout.amount().amount(),
                payout.amount().currency().getCurrencyCode(),
                payout.workerId().value(),
                payout.receivedAt(),
                payout.status());
    }
}
