package com.casestudies.payoutdelivery.fakebank.bank.application.port.in.Command;

import java.util.Objects;

import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.Money;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.WorkerId;

public record CreateBankPayoutCommand(PayoutId payoutId, MerchantId merchantId, Money amount, WorkerId workerId) {

    public CreateBankPayoutCommand {
        Objects.requireNonNull(payoutId, "payout id is required");
        Objects.requireNonNull(merchantId, "merchant id is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(workerId, "worker id is required");
    }
}
