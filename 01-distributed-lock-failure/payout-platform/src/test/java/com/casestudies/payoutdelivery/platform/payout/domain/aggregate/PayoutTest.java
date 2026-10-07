package com.casestudies.payoutdelivery.platform.payout.domain.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.casestudies.payoutdelivery.platform.payout.domain.exception.IllegalPayoutTransitionException;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.Money;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutStatus;

class PayoutTest {

    private static final PayoutId PAYOUT_ID = PayoutId.of("PO-9001");
    private static final MerchantId MERCHANT = MerchantId.of("M-1001");
    private static final Money AMOUNT = Money.of("252000", "EGP");

    @Test
    void preparedPayoutIsReadyWithItsMerchantAndAmount() {
        Payout payout = Payout.prepare(PAYOUT_ID, MERCHANT, AMOUNT);

        assertThat(payout.id()).isEqualTo(PAYOUT_ID);
        assertThat(payout.merchantId()).isEqualTo(MERCHANT);
        assertThat(payout.amount()).isEqualTo(Money.of("252000.00", "EGP"));
        assertThat(payout.status()).isEqualTo(PayoutStatus.READY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void rejectsNonPositiveAmount(String amount) {
        assertThatThrownBy(() -> Payout.prepare(PAYOUT_ID, MERCHANT, Money.of(amount, "EGP")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }

    @Test
    void rejectsMissingParts() {
        assertThatThrownBy(() -> Payout.prepare(null, MERCHANT, AMOUNT)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Payout.prepare(PAYOUT_ID, null, AMOUNT)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Payout.prepare(PAYOUT_ID, MERCHANT, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void dispatchSucceeds() {
        Payout payout = Payout.prepare(PAYOUT_ID, MERCHANT, AMOUNT);

        payout.startDispatch();
        assertThat(payout.status()).isEqualTo(PayoutStatus.DISPATCHING);

        payout.markDispatched();
        assertThat(payout.status()).isEqualTo(PayoutStatus.DISPATCHED);
    }

    @Test
    void dispatchFails() {
        Payout payout = Payout.prepare(PAYOUT_ID, MERCHANT, AMOUNT);

        payout.startDispatch();
        payout.markFailed();

        assertThat(payout.status()).isEqualTo(PayoutStatus.FAILED);
    }

    @Test
    void cannotStartDispatchTwice() {
        Payout payout = Payout.prepare(PAYOUT_ID, MERCHANT, AMOUNT);
        payout.startDispatch();

        assertThatThrownBy(payout::startDispatch)
                .isInstanceOfSatisfying(IllegalPayoutTransitionException.class, e -> {
                    assertThat(e.payoutId()).isEqualTo(PAYOUT_ID);
                    assertThat(e.from()).isEqualTo(PayoutStatus.DISPATCHING);
                    assertThat(e.to()).isEqualTo(PayoutStatus.DISPATCHING);
                });
    }

    @Test
    void cannotRedispatchADispatchedPayout() {
        Payout payout = Payout.prepare(PAYOUT_ID, MERCHANT, AMOUNT);
        payout.startDispatch();
        payout.markDispatched();

        assertThatThrownBy(payout::startDispatch).isInstanceOf(IllegalPayoutTransitionException.class);
        assertThatThrownBy(payout::markFailed).isInstanceOf(IllegalPayoutTransitionException.class);
        assertThat(payout.status()).isEqualTo(PayoutStatus.DISPATCHED);
    }

    @Test
    void cannotFinishADispatchThatNeverStarted() {
        Payout payout = Payout.prepare(PAYOUT_ID, MERCHANT, AMOUNT);

        assertThatThrownBy(payout::markDispatched).isInstanceOf(IllegalPayoutTransitionException.class);
        assertThatThrownBy(payout::markFailed).isInstanceOf(IllegalPayoutTransitionException.class);
        assertThat(payout.status()).isEqualTo(PayoutStatus.READY);
    }

    @Test
    void payoutsWithTheSameIdAreTheSamePayout() {
        Payout first = Payout.prepare(PAYOUT_ID, MERCHANT, AMOUNT);
        Payout second = Payout.prepare(PAYOUT_ID, MERCHANT, AMOUNT);
        second.startDispatch();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(Payout.prepare(PayoutId.of("PO-9002"), MERCHANT, AMOUNT));
    }
}
