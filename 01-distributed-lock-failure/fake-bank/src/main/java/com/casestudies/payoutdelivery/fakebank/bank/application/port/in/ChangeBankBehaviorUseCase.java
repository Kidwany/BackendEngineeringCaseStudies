package com.casestudies.payoutdelivery.fakebank.bank.application.port.in;

import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.Command.ChangeBankBehaviorCommand;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehavior;

public interface ChangeBankBehaviorUseCase {

    BankBehavior change(ChangeBankBehaviorCommand command);
}
