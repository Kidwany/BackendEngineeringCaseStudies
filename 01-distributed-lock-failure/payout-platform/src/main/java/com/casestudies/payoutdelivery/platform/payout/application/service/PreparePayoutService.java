package com.casestudies.payoutdelivery.platform.payout.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.casestudies.payoutdelivery.platform.payout.application.port.in.PreparePayoutUseCase;
import com.casestudies.payoutdelivery.platform.payout.application.port.in.PreparedPayout;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxRepository;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.PayoutRepository;
import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.event.PayoutReady;

// The payout and its PayoutReady event commit together or not at all. Nothing here talks to
// Kafka: the outbox publisher sends the event later, after this transaction has committed.
public class PreparePayoutService implements PreparePayoutUseCase {

    private final PayoutRepository payouts;
    private final OutboxRepository outbox;
    private final Clock clock;

    public PreparePayoutService(PayoutRepository payouts, OutboxRepository outbox, Clock clock) {
        this.payouts = payouts;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PreparedPayout prepare() {
        Payout payout = ScenarioPayout.initialState();
        payouts.save(payout);

        PayoutReady event = PayoutReady.of(payout, UUID.randomUUID(), clock.instant());
        outbox.append(event);

        return new PreparedPayout(payout, event);
    }
}
