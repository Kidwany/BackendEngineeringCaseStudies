package com.casestudies.payoutdelivery.platform.payout.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.casestudies.payoutdelivery.platform.payout.application.port.out.PayoutRepository;
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

    private static final class InMemoryPayoutRepository implements PayoutRepository {

        private final Map<PayoutId, Payout> rows = new HashMap<>();

        @Override
        public Optional<Payout> findById(PayoutId id) {
            Payout row = rows.get(id);
            return row == null ? Optional.empty()
                    : Optional.of(Payout.restore(row.id(), row.merchantId(), row.amount(), row.status()));
        }

        @Override
        public void save(Payout payout) {
            rows.put(payout.id(), Payout.restore(payout.id(), payout.merchantId(), payout.amount(), payout.status()));
        }

        int count() {
            return rows.size();
        }
    }
}
