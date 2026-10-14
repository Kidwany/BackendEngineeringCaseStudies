package com.casestudies.payoutdelivery.fakebank.bank.application.service;

import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.Command.ChangeBankBehaviorCommand;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.ChangeBankBehaviorUseCase;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.GetBankBehaviorUseCase;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.BankBehaviorStore;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehavior;

public class BankBehaviorService implements GetBankBehaviorUseCase, ChangeBankBehaviorUseCase {

    private final BankBehaviorStore behaviors;

    public BankBehaviorService(BankBehaviorStore behaviors) {
        this.behaviors = behaviors;
    }

    @Override
    public BankBehavior current() {
        return behaviors.current();
    }

    @Override
    public BankBehavior change(ChangeBankBehaviorCommand command) {
        BankBehavior changed = new BankBehavior(command.mode(), command.delay().orElse(behaviors.current().delay()));
        behaviors.replace(changed);
        return changed;
    }
}
