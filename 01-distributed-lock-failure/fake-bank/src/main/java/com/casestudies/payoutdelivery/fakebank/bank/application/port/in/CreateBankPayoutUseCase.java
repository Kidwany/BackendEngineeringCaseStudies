package com.casestudies.payoutdelivery.fakebank.bank.application.port.in;

import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.Command.CreateBankPayoutCommand;
import com.casestudies.payoutdelivery.fakebank.bank.domain.aggregate.BankPayout;

public interface CreateBankPayoutUseCase {

    BankPayout create(CreateBankPayoutCommand command);
}
