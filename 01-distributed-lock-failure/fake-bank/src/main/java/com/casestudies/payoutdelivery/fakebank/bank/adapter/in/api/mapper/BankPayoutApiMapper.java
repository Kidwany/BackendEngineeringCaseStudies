package com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.mapper;

import com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto.BankPayoutResponse;
import com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto.CreateBankPayoutRequest;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.Command.CreateBankPayoutCommand;
import com.casestudies.payoutdelivery.fakebank.bank.domain.aggregate.BankPayout;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.Money;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.WorkerId;

public final class BankPayoutApiMapper {

    private BankPayoutApiMapper() {
    }

    public static CreateBankPayoutCommand toCommand(CreateBankPayoutRequest request, String workerId) {
        return new CreateBankPayoutCommand(
                PayoutId.of(request.payoutId()),
                MerchantId.of(request.merchantId()),
                Money.of(request.amount().toPlainString(), request.currency()),
                WorkerId.of(workerId));
    }

    public static BankPayoutResponse toResponse(BankPayout payout) {
        return new BankPayoutResponse(
                payout.id().value(),
                payout.payoutId().value(),
                payout.merchantId().value(),
                payout.amount().amount(),
                payout.amount().currency().getCurrencyCode(),
                payout.workerId().value(),
                payout.receivedAt(),
                payout.status().name());
    }
}
