package com.casestudies.payoutdelivery.fakebank.bank.adapter.out.memory;

import java.util.concurrent.atomic.AtomicReference;

import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.BankBehaviorStore;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehavior;

public class InMemoryBankBehaviorStore implements BankBehaviorStore {

    private final AtomicReference<BankBehavior> behavior;

    public InMemoryBankBehaviorStore(BankBehavior initial) {
        this.behavior = new AtomicReference<>(initial);
    }

    @Override
    public BankBehavior current() {
        return behavior.get();
    }

    @Override
    public void replace(BankBehavior behavior) {
        this.behavior.set(behavior);
    }
}
