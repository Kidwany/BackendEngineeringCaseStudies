package com.casestudies.payoutdelivery.platform.payout.application.port.in;

import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.event.PayoutReady;

public record PreparedPayout(Payout payout, PayoutReady event) {
}
