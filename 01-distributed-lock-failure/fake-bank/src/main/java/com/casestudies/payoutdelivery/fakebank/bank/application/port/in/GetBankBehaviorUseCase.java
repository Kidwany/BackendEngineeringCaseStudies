package com.casestudies.payoutdelivery.fakebank.bank.application.port.in;

import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehavior;

public interface GetBankBehaviorUseCase {

    BankBehavior current();
}
