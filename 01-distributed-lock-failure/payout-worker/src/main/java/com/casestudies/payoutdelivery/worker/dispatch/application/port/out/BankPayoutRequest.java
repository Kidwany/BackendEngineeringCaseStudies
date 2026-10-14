package com.casestudies.payoutdelivery.worker.dispatch.application.port.out;

import java.util.Objects;

import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.Money;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.WorkerId;

public record BankPayoutRequest(PayoutId payoutId, MerchantId merchantId, Money amount, WorkerId workerId) {

    public BankPayoutRequest {
        Objects.requireNonNull(payoutId, "payout id is required");
        Objects.requireNonNull(merchantId, "merchant id is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(workerId, "worker id is required");
    }
}
