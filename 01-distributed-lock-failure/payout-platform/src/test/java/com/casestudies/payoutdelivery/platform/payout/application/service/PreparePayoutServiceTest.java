package com.casestudies.payoutdelivery.platform.payout.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.casestudies.payoutdelivery.platform.payout.application.port.in.PreparedPayout;
import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutStatus;

class PreparePayoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

    private final InMemoryPayoutRepository payouts = new InMemoryPayoutRepository();
    private final InMemoryOutboxRepository outbox = new InMemoryOutboxRepository();
    private final PreparePayoutService service =
            new PreparePayoutService(payouts, outbox, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void preparesTheScenarioPayoutAndRecordsOneEventForIt() {
        PreparedPayout prepared = service.prepare();

        Payout stored = payouts.findById(ScenarioPayout.PAYOUT_ID).orElseThrow();
        assertThat(stored.status()).isEqualTo(PayoutStatus.READY);
        assertThat(stored.amount()).isEqualTo(ScenarioPayout.AMOUNT);

        assertThat(outbox.appended()).singleElement().satisfies(event -> {
            assertThat(event.payoutId()).isEqualTo(ScenarioPayout.PAYOUT_ID);
            assertThat(event.merchantId()).isEqualTo(ScenarioPayout.MERCHANT_ID);
            assertThat(event.amount()).isEqualTo(ScenarioPayout.AMOUNT);
            assertThat(event.occurredAt()).isEqualTo(NOW);
        });
        assertThat(prepared.event()).isEqualTo(outbox.appended().getFirst());
        assertThat(prepared.payout().id()).isEqualTo(ScenarioPayout.PAYOUT_ID);
    }

    @Test
    void preparingADispatchedPayoutPutsItBackToReady() {
        Payout dispatched = ScenarioPayout.initialState();
        dispatched.startDispatch();
        dispatched.markDispatched();
        payouts.save(dispatched);

        service.prepare();

        assertThat(payouts.findById(ScenarioPayout.PAYOUT_ID).orElseThrow().status()).isEqualTo(PayoutStatus.READY);
    }

    @Test
    void everyPreparationIsANewEvent() {
        service.prepare();
        service.prepare();

        assertThat(outbox.appended()).hasSize(2);
        assertThat(outbox.appended().get(0).eventId()).isNotEqualTo(outbox.appended().get(1).eventId());
        assertThat(payouts.count()).isEqualTo(1);
    }
}
