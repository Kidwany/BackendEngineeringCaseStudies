package com.casestudies.payoutdelivery.fakebank.bank.domain.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankPayoutId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankPayoutStatus;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.Money;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.WorkerId;

class BankPayoutTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    @Test
    void aNewBankPayoutIsCreated() {
        BankPayoutId id = BankPayoutId.newId();

        BankPayout payout = create(id, Money.of("252000", "EGP"));

        assertThat(payout.id()).isEqualTo(id);
        assertThat(payout.payoutId()).isEqualTo(PayoutId.of("PO-9001"));
        assertThat(payout.merchantId()).isEqualTo(MerchantId.of("M-1001"));
        assertThat(payout.amount()).isEqualTo(Money.of("252000", "EGP"));
        assertThat(payout.workerId()).isEqualTo(WorkerId.of("worker-b"));
        assertThat(payout.receivedAt()).isEqualTo(NOW);
        assertThat(payout.status()).isEqualTo(BankPayoutStatus.CREATED);
    }

    @Test
    void rejectsANonPositiveAmount() {
        assertThatThrownBy(() -> create(BankPayoutId.newId(), Money.of("0", "EGP")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void twoBankPayoutsForTheSamePayoutAreDifferentPayouts() {
        BankPayout first = create(BankPayoutId.newId(), Money.of("252000", "EGP"));
        BankPayout second = create(BankPayoutId.newId(), Money.of("252000", "EGP"));

        assertThat(first).isNotEqualTo(second);
    }

    private static BankPayout create(BankPayoutId id, Money amount) {
        return BankPayout.create(id, PayoutId.of("PO-9001"), MerchantId.of("M-1001"), amount,
                WorkerId.of("worker-b"), NOW);
    }
}
