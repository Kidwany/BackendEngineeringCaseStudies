package com.casestudies.payoutdelivery.fakebank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BankSchemaTest {

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void startEmpty() {
        jdbc.sql("DELETE FROM bank_payouts").update();
    }

    @Test
    void allowsDuplicatePayoutRows() {
        insert("PO-9001", 252000, "CREATED");
        insert("PO-9001", 252000, "CREATED");

        assertThat(jdbc.sql("SELECT count(*) FROM bank_payouts WHERE payout_id = 'PO-9001'")
                .query(Long.class).single()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidRows() {
        assertThatThrownBy(() -> insert("PO-9001", 0, "CREATED"))
                .hasMessageContaining("bank_payouts_amount_positive");
        assertThatThrownBy(() -> insert("PO-9001", 252000, "IDEMPOTENT_REPLAY"))
                .hasMessageContaining("bank_payouts_status_known");
    }

    @Test
    void indexesPayoutIdForDuplicateLookups() {
        assertThat(jdbc.sql("SELECT indexname FROM pg_indexes WHERE tablename = 'bank_payouts'")
                .query(String.class).list()).contains("bank_payouts_payout_id_idx");
    }

    private void insert(String payoutId, long amount, String status) {
        jdbc.sql("""
                INSERT INTO bank_payouts (id, payout_id, merchant_id, amount, currency, worker_id, received_at, status)
                VALUES (gen_random_uuid(), :payoutId, 'M-1001', :amount, 'EGP', 'worker-a', now(), :status)""")
                .param("payoutId", payoutId).param("amount", amount).param("status", status)
                .update();
    }
}
