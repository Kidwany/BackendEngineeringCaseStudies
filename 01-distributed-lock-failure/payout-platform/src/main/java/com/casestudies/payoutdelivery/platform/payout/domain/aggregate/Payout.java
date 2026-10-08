package com.casestudies.payoutdelivery.platform.payout.domain.aggregate;

import static com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutStatus.DISPATCHED;
import static com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutStatus.DISPATCHING;
import static com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutStatus.FAILED;
import static com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutStatus.READY;

import java.util.Objects;

import com.casestudies.payoutdelivery.platform.payout.domain.exception.IllegalPayoutTransitionException;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.Money;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.platform.payout.domain.valueobject.PayoutStatus;

public class Payout {

    private final PayoutId id;
    private final MerchantId merchantId;
    private final Money amount;
    private PayoutStatus status;

    private Payout(PayoutId id, MerchantId merchantId, Money amount, PayoutStatus status) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.merchantId = Objects.requireNonNull(merchantId, "merchant id is required");
        this.amount = Objects.requireNonNull(amount, "amount is required");
        this.status = Objects.requireNonNull(status, "status is required");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("payout amount must be positive, was " + amount);
        }
    }

    public static Payout prepare(PayoutId id, MerchantId merchantId, Money amount) {
        return new Payout(id, merchantId, amount, READY);
    }

    public static Payout restore(PayoutId id, MerchantId merchantId, Money amount, PayoutStatus status) {
        return new Payout(id, merchantId, amount, status);
    }

    public void startDispatch() {
        transition(READY, DISPATCHING);
    }

    public void markDispatched() {
        transition(DISPATCHING, DISPATCHED);
    }

    public void markFailed() {
        transition(DISPATCHING, FAILED);
    }

    private void transition(PayoutStatus expected, PayoutStatus next) {
        if (status != expected) {
            throw new IllegalPayoutTransitionException(id, status, next);
        }
        status = next;
    }

    public PayoutId id() {
        return id;
    }

    public MerchantId merchantId() {
        return merchantId;
    }

    public Money amount() {
        return amount;
    }

    public PayoutStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Payout other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Payout[" + id + ", " + merchantId + ", " + amount + ", " + status + "]";
    }
}
