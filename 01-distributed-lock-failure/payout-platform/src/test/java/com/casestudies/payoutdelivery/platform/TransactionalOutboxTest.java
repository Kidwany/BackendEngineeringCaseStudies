package com.casestudies.payoutdelivery.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.casestudies.payoutdelivery.platform.payout.application.port.in.PreparePayoutUseCase;
import com.casestudies.payoutdelivery.platform.payout.application.port.in.PreparedPayout;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TransactionalOutboxTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PreparePayoutUseCase preparePayout;

    @BeforeEach
    void startEmpty() {
        jdbc.sql("DELETE FROM payout_dispatch_attempts").update();
        jdbc.sql("DELETE FROM outbox_events").update();
        jdbc.sql("DELETE FROM payouts").update();
    }

    @Test
    void commitsThePayoutAndAPendingPayoutReadyEventTogether() {
        PreparedPayout prepared = preparePayout.prepare();

        assertThat(jdbc.sql("SELECT status FROM payouts WHERE id = 'PO-9001'").query(String.class).single())
                .isEqualTo("READY");

        var event = jdbc.sql("""
                SELECT id, aggregate_id, event_type, status, published_at, created_at,
                       payload->>'eventId' AS payload_event_id, payload->>'payoutId' AS payload_payout_id,
                       payload->>'merchantId' AS payload_merchant_id, payload->>'amount' AS payload_amount,
                       payload->>'currency' AS payload_currency, payload->>'occurredAt' AS payload_occurred_at
                FROM outbox_events""").query().singleRow();

        assertThat(event.get("id")).isEqualTo(prepared.event().eventId());
        assertThat(event.get("aggregate_id")).isEqualTo("PO-9001");
        assertThat(event.get("event_type")).isEqualTo("PayoutReady");
        assertThat(event.get("status")).isEqualTo("PENDING");
        assertThat(event.get("published_at")).isNull();
        assertThat(event.get("created_at")).isNotNull();

        assertThat(event.get("payload_event_id")).isEqualTo(prepared.event().eventId().toString());
        assertThat(event.get("payload_payout_id")).isEqualTo("PO-9001");
        assertThat(event.get("payload_merchant_id")).isEqualTo("M-1001");
        assertThat(new BigDecimal((String) event.get("payload_amount"))).isEqualByComparingTo("252000");
        assertThat(event.get("payload_currency")).isEqualTo("EGP");
        assertThat(event.get("payload_occurred_at")).isNotNull();
    }

    @Test
    void preparingAgainAddsANewEventButKeepsOnePayout() {
        preparePayout.prepare();
        preparePayout.prepare();

        assertThat(jdbc.sql("SELECT count(*) FROM payouts").query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE status = 'PENDING'").query(Long.class).single())
                .isEqualTo(2);
    }
}
