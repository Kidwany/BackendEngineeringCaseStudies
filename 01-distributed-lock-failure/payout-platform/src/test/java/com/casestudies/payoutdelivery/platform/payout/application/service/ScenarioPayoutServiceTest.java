package com.casestudies.payoutdelivery.platform.payout.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.Money;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutStatus;

class ScenarioPayoutServiceTest {

    private final InMemoryPayoutRepository payouts = new InMemoryPayoutRepository();
    private final ScenarioPayoutService service = new ScenarioPayoutService(payouts);

    @Test
    void scenarioPayoutIsTheDocumentedOne() {
        assertThat(ScenarioPayout.PAYOUT_ID).isEqualTo(PayoutId.of("PO-9001"));
        assertThat(ScenarioPayout.MERCHANT_ID).isEqualTo(MerchantId.of("M-1001"));
        assertThat(ScenarioPayout.AMOUNT).isEqualTo(Money.of("252000", "EGP"));
    }

    @Test
    void seedCreatesTheScenarioPayoutWhenMissing() {
        service.seed();

        assertInitialState(payouts.findById(ScenarioPayout.PAYOUT_ID).orElseThrow());
    }

    @Test
    void seedLeavesAnExistingPayoutAlone() {
        Payout inFlight = ScenarioPayout.initialState();
        inFlight.startDispatch();
        payouts.save(inFlight);

        service.seed();

        assertThat(payouts.findById(ScenarioPayout.PAYOUT_ID).orElseThrow().status())
                .isEqualTo(PayoutStatus.DISPATCHING);
    }

    @Test
    void resetRestoresTheExactInitialStateFromAnyState() {
        Payout dispatched = ScenarioPayout.initialState();
        dispatched.startDispatch();
        dispatched.markDispatched();
        payouts.save(dispatched);

        service.reset();

        assertInitialState(payouts.findById(ScenarioPayout.PAYOUT_ID).orElseThrow());
    }

    @Test
    void resetIsRepeatable() {
        service.reset();
        service.reset();

        assertInitialState(payouts.findById(ScenarioPayout.PAYOUT_ID).orElseThrow());
        assertThat(payouts.count()).isEqualTo(1);
    }

    private static void assertInitialState(Payout payout) {
        assertThat(payout.id()).isEqualTo(PayoutId.of("PO-9001"));
        assertThat(payout.merchantId()).isEqualTo(MerchantId.of("M-1001"));
        assertThat(payout.amount()).isEqualTo(Money.of("252000", "EGP"));
        assertThat(payout.status()).isEqualTo(PayoutStatus.READY);
    }
}
