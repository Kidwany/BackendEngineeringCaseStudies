package com.casestudies.payoutdelivery.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PayoutSchemaTest {

    @Autowired
    private JdbcClient jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.sql("DELETE FROM payout_dispatch_attempts").update();
        jdbc.sql("DELETE FROM outbox_events").update();
        jdbc.sql("DELETE FROM payouts").update();
    }

    @Test
    void storesAPayoutWithExactAmountAndTimestamps() {
        insertPayout("PO-9001", "252000.00", "READY");

        var row = jdbc.sql("SELECT amount, currency, status, created_at, updated_at FROM payouts WHERE id = 'PO-9001'")
                .query().singleRow();

        assertThat((BigDecimal) row.get("amount")).isEqualByComparingTo("252000.00");
        assertThat(row.get("currency")).isEqualTo("EGP");
        assertThat(row.get("status")).isEqualTo("READY");
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    void rejectsInvalidPayouts() {
        assertThatThrownBy(() -> insertPayout("PO-1", "0", "READY"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("payouts_amount_positive");
        assertThatThrownBy(() -> insertPayout("PO-2", "10", "SENT"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("payouts_status_known");

        insertPayout("PO-9001", "252000", "READY");
        assertThatThrownBy(() -> insertPayout("PO-9001", "252000", "READY"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("payouts_pkey");
    }

    @Test
    void outboxEventStartsPendingAndNeedsATimestampToBePublished() {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO outbox_events (id, aggregate_id, event_type, payload)
                VALUES (:id, 'PO-9001', 'PayoutReady', CAST(:payload AS jsonb))""")
                .param("id", id).param("payload", "{\"payoutId\":\"PO-9001\"}").update();

        assertThat(jdbc.sql("SELECT status FROM outbox_events WHERE id = :id").param("id", id)
                .query(String.class).single()).isEqualTo("PENDING");

        assertThatThrownBy(() -> jdbc.sql("UPDATE outbox_events SET status = 'PUBLISHED' WHERE id = :id")
                .param("id", id).update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("outbox_events_published_at_matches_status");

        jdbc.sql("UPDATE outbox_events SET status = 'PUBLISHED', published_at = now() WHERE id = :id")
                .param("id", id).update();
    }

    @Test
    void dispatchAttemptsBelongToAnExistingPayoutAndAllowAMissingFencingToken() {
        assertThatThrownBy(() -> insertAttempt("PO-404", "STARTED"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("payout_dispatch_attempts_payout_id_fkey");

        insertPayout("PO-9001", "252000", "READY");
        insertAttempt("PO-9001", "STARTED");
        insertAttempt("PO-9001", "SENT_TO_BANK");

        assertThat(jdbc.sql("SELECT count(*) FROM payout_dispatch_attempts WHERE payout_id = 'PO-9001' AND fencing_token IS NULL")
                .query(Long.class).single()).isEqualTo(2);

        assertThatThrownBy(() -> insertAttempt("PO-9001", "DONE"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("payout_dispatch_attempts_status_known");
    }

    @Test
    void indexesExist() {
        var indexes = jdbc.sql("SELECT indexname FROM pg_indexes WHERE schemaname = 'public'")
                .query(String.class).set();

        assertThat(indexes).contains(
                "payouts_merchant_id_idx",
                "outbox_events_pending_idx",
                "outbox_events_aggregate_id_idx",
                "payout_dispatch_attempts_payout_id_idx");
    }

    private void insertPayout(String id, String amount, String status) {
        jdbc.sql("""
                INSERT INTO payouts (id, merchant_id, amount, currency, status)
                VALUES (:id, 'M-1001', :amount, 'EGP', :status)""")
                .param("id", id).param("amount", new BigDecimal(amount)).param("status", status).update();
    }

    private void insertAttempt(String payoutId, String status) {
        jdbc.sql("""
                INSERT INTO payout_dispatch_attempts (id, payout_id, worker_id, strategy, status, started_at)
                VALUES (:id, :payoutId, 'worker-a', 'BROKEN', :status, now())""")
                .param("id", UUID.randomUUID()).param("payoutId", payoutId).param("status", status).update();
    }
}
