package com.casestudies.payoutdelivery.fakebank.bank.domain.aggregate;

import java.time.Instant;
import java.util.Objects;

import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankPayoutId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankPayoutStatus;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.Money;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.WorkerId;

public class BankPayout {

    private final BankPayoutId id;
    private final PayoutId payoutId;
    private final MerchantId merchantId;
    private final Money amount;
    private final WorkerId workerId;
    private final Instant receivedAt;
    private final BankPayoutStatus status;

    private BankPayout(BankPayoutId id, PayoutId payoutId, MerchantId merchantId, Money amount, WorkerId workerId,
                       Instant receivedAt, BankPayoutStatus status) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.payoutId = Objects.requireNonNull(payoutId, "payout id is required");
        this.merchantId = Objects.requireNonNull(merchantId, "merchant id is required");
        this.amount = Objects.requireNonNull(amount, "amount is required");
        this.workerId = Objects.requireNonNull(workerId, "worker id is required");
        this.receivedAt = Objects.requireNonNull(receivedAt, "received at is required");
        this.status = Objects.requireNonNull(status, "status is required");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("bank payout amount must be positive, was " + amount);
        }
    }

    public static BankPayout create(BankPayoutId id, PayoutId payoutId, MerchantId merchantId, Money amount,
                                    WorkerId workerId, Instant receivedAt) {
        return new BankPayout(id, payoutId, merchantId, amount, workerId, receivedAt, BankPayoutStatus.CREATED);
    }

    public BankPayoutId id() {
        return id;
    }

    public PayoutId payoutId() {
        return payoutId;
    }

    public MerchantId merchantId() {
        return merchantId;
    }

    public Money amount() {
        return amount;
    }

    public WorkerId workerId() {
        return workerId;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    public BankPayoutStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof BankPayout other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "BankPayout[" + id + ", " + payoutId + ", " + merchantId + ", " + amount + ", " + workerId + ", "
                + status + "]";
    }
}
