package com.casestudies.payoutdelivery.platform.payout.application.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.casestudies.payoutdelivery.platform.payout.application.port.out.PayoutRepository;
import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;

// Copies on the way in and out, like a real database, so a test cannot change a stored
// payout by mutating the object it saved.
class InMemoryPayoutRepository implements PayoutRepository {

    private final Map<PayoutId, Payout> rows = new HashMap<>();

    @Override
    public Optional<Payout> findById(PayoutId id) {
        return Optional.ofNullable(rows.get(id)).map(InMemoryPayoutRepository::copy);
    }

    @Override
    public void save(Payout payout) {
        rows.put(payout.id(), copy(payout));
    }

    int count() {
        return rows.size();
    }

    private static Payout copy(Payout payout) {
        return Payout.restore(payout.id(), payout.merchantId(), payout.amount(), payout.status());
    }
}
