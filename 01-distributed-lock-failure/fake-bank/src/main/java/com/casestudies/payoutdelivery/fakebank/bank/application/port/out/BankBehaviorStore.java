package com.casestudies.payoutdelivery.fakebank.bank.application.port.out;

import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehavior;

public interface BankBehaviorStore {

    BankBehavior current();

    void replace(BankBehavior behavior);
}
