package com.casestudies.payoutdelivery.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.casestudies.payoutdelivery.platform.payout.application.port.in.PreparePayoutUseCase;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxRepository;

// The outbox write fails after the payout row was already written in the same transaction.
// If the two were not atomic, the payout would now be READY with no event to announce it.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TransactionalOutboxRollbackTest {

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PreparePayoutUseCase preparePayout;

    @MockitoSpyBean
    private OutboxRepository outbox;

    @BeforeEach
    void dispatchedPayoutAndEmptyOutbox() {
        jdbc.sql("DELETE FROM payout_dispatch_attempts").update();
        jdbc.sql("DELETE FROM outbox_events").update();
        jdbc.sql("DELETE FROM payouts").update();
        jdbc.sql("""
                INSERT INTO payouts (id, merchant_id, amount, currency, status)
                VALUES ('PO-9001', 'M-1001', 252000, 'EGP', 'DISPATCHED')""").update();
    }

    @Test
    void aFailedOutboxWriteRollsBackThePayoutChange() {
        doThrow(new IllegalStateException("outbox write failed")).when(outbox).append(any());

        assertThatThrownBy(preparePayout::prepare).hasMessage("outbox write failed");

        assertThat(jdbc.sql("SELECT status FROM payouts WHERE id = 'PO-9001'").query(String.class).single())
                .isEqualTo("DISPATCHED");
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events").query(Long.class).single()).isZero();
    }
}
