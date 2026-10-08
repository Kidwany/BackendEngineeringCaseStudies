package com.casestudies.payoutdelivery.platform.payout.application.port.out;

import java.util.Optional;

import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;

public interface PayoutRepository {

    Optional<Payout> findById(PayoutId id);

    void save(Payout payout);
}
