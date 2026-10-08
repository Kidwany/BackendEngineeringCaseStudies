package com.casestudies.payoutdelivery.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.casestudies.payoutdelivery.platform.payout.application.port.in.ResetScenarioPayoutUseCase;
import com.casestudies.payoutdelivery.platform.payout.application.port.in.SeedScenarioPayoutUseCase;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.PayoutRepository;
import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ScenarioPayoutPersistenceTest {

    private static final PayoutId PO_9001 = PayoutId.of("PO-9001");

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PayoutRepository payouts;

    @Autowired
    private SeedScenarioPayoutUseCase seed;

    @Autowired
    private ResetScenarioPayoutUseCase reset;

    @Autowired
    private List<ApplicationRunner> runners;

    @BeforeEach
    void startEmpty() {
        jdbc.sql("DELETE FROM payout_dispatch_attempts").update();
        jdbc.sql("DELETE FROM outbox_events").update();
        jdbc.sql("DELETE FROM payouts").update();
    }

    @Test
    void startupSeedsTheScenarioPayout() throws Exception {
        ApplicationArguments noArgs = new DefaultApplicationArguments();
        for (ApplicationRunner runner : runners) {
            runner.run(noArgs);
        }

        assertRowIsInitialState();
    }

    @Test
    void seedDoesNotOverwriteAPayoutInProgress() {
        seed.seed();
        Payout payout = payouts.findById(PO_9001).orElseThrow();
        payout.startDispatch();
        payouts.save(payout);

        seed.seed();

        assertThat(jdbc.sql("SELECT status FROM payouts WHERE id = 'PO-9001'").query(String.class).single())
                .isEqualTo("DISPATCHING");
    }

    @Test
    void resetRecreatesTheExactRowAfterADispatch() {
        seed.seed();
        Payout payout = payouts.findById(PO_9001).orElseThrow();
        payout.startDispatch();
        payout.markDispatched();
        payouts.save(payout);

        reset.reset();

        assertRowIsInitialState();
    }

    @Test
    void resetRecreatesTheRowWhenItWasDeleted() {
        reset.reset();

        assertRowIsInitialState();
    }

    private void assertRowIsInitialState() {
        var rows = jdbc.sql("SELECT id, merchant_id, amount, currency, status FROM payouts").query().listOfRows();

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("id")).isEqualTo("PO-9001");
            assertThat(row.get("merchant_id")).isEqualTo("M-1001");
            assertThat((BigDecimal) row.get("amount")).isEqualByComparingTo("252000");
            assertThat(row.get("currency")).isEqualTo("EGP");
            assertThat(row.get("status")).isEqualTo("READY");
        });
    }
}
