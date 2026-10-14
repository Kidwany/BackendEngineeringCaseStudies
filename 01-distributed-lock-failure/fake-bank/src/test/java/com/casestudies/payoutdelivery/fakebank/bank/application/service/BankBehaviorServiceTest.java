package com.casestudies.payoutdelivery.fakebank.bank.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.casestudies.payoutdelivery.fakebank.bank.adapter.out.memory.InMemoryBankBehaviorStore;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.Command.ChangeBankBehaviorCommand;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehavior;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehaviorMode;

class BankBehaviorServiceTest {

    private final BankBehaviorService service =
            new BankBehaviorService(new InMemoryBankBehaviorStore(BankBehavior.normal(Duration.ofSeconds(5))));

    @Test
    void startsWithTheInitialBehavior() {
        assertThat(service.current()).isEqualTo(BankBehavior.normal(Duration.ofSeconds(5)));
    }

    @Test
    void changesModeAndDelay() {
        BankBehavior changed = service.change(new ChangeBankBehaviorCommand(
                BankBehaviorMode.TIMEOUT, Optional.of(Duration.ofSeconds(30))));

        assertThat(changed).isEqualTo(new BankBehavior(BankBehaviorMode.TIMEOUT, Duration.ofSeconds(30)));
        assertThat(service.current()).isEqualTo(changed);
    }

    @Test
    void keepsTheCurrentDelayWhenNoneIsGiven() {
        service.change(new ChangeBankBehaviorCommand(BankBehaviorMode.DELAY_RESPONSE, Optional.of(Duration.ofSeconds(12))));

        BankBehavior changed = service.change(new ChangeBankBehaviorCommand(BankBehaviorMode.NORMAL, Optional.empty()));

        assertThat(changed).isEqualTo(BankBehavior.normal(Duration.ofSeconds(12)));
    }
}
