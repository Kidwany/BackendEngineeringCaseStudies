package com.casestudies.payoutdelivery.fakebank.bank.application.service;

import java.time.Clock;

import org.springframework.transaction.annotation.Transactional;

import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.CreateBankPayoutCommand;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.CreateBankPayoutUseCase;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.BankPayoutRepository;
import com.casestudies.payoutdelivery.fakebank.bank.domain.aggregate.BankPayout;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankPayoutId;

public class CreateBankPayoutService implements CreateBankPayoutUseCase {

    private final BankPayoutRepository payouts;
    private final Clock clock;

    public CreateBankPayoutService(BankPayoutRepository payouts, Clock clock) {
        this.payouts = payouts;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BankPayout create(CreateBankPayoutCommand command) {
        BankPayout payout = BankPayout.create(BankPayoutId.newId(), command.payoutId(), command.merchantId(),
                command.amount(), command.workerId(), clock.instant());
        payouts.save(payout);
        return payout;
    }
}
