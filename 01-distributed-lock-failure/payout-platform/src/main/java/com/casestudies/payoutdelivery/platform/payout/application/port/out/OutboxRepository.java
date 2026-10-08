package com.casestudies.payoutdelivery.platform.payout.application.port.out;

import com.casestudies.payoutdelivery.platform.payout.domain.event.PayoutReady;

public interface OutboxRepository {

    void append(PayoutReady event);
}
