package com.casestudies.payoutdelivery.platform.payout.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.casestudies.payoutdelivery.platform.payout.domain.aggregate.Payout;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.Money;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;

class PayoutReadyTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000005512");
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

    private final Payout payout = Payout.prepare(PayoutId.of("PO-9001"), MerchantId.of("M-1001"), Money.of("252000", "EGP"));

    @Test
    void carriesWhatTheWorkerNeedsToPayTheMerchant() {
        PayoutReady event = PayoutReady.of(payout, EVENT_ID, NOW);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.payoutId()).isEqualTo(PayoutId.of("PO-9001"));
        assertThat(event.merchantId()).isEqualTo(MerchantId.of("M-1001"));
        assertThat(event.amount()).isEqualTo(Money.of("252000", "EGP"));
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void onlyAReadyPayoutCanBeAnnounced() {
        payout.startDispatch();

        assertThatThrownBy(() -> PayoutReady.of(payout, EVENT_ID, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DISPATCHING");
    }
}
