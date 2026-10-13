package com.casestudies.payoutdelivery.fakebank.bank.application.port.out;

import com.casestudies.payoutdelivery.fakebank.bank.domain.aggregate.BankPayout;

public interface BankPayoutRepository {

    void save(BankPayout payout);
}
