package com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.mapper;

import java.util.Currency;

import com.casestudies.payoutdelivery.platform.payout.adapter.out.persistence.Entity.PayoutJpaEntity;
import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.Money;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;

public final class PayoutPersistenceMapper {

    private PayoutPersistenceMapper() {
    }

    public static PayoutJpaEntity toEntity(Payout payout) {
        return new PayoutJpaEntity(
                payout.id().value(),
                payout.merchantId().value(),
                payout.amount().amount(),
                payout.amount().currency().getCurrencyCode(),
                payout.status());
    }

    public static Payout toDomain(PayoutJpaEntity entity) {
        return Payout.restore(
                PayoutId.of(entity.getId()),
                MerchantId.of(entity.getMerchantId()),
                new Money(entity.getAmount(), Currency.getInstance(entity.getCurrency())),
                entity.getStatus());
    }
}
