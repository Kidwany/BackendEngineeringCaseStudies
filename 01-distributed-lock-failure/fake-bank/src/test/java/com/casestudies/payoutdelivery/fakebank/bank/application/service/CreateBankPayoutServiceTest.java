package com.casestudies.payoutdelivery.fakebank.bank.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.casestudies.payoutdelivery.fakebank.bank.adapter.out.memory.InMemoryBankBehaviorStore;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.Command.CreateBankPayoutCommand;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.BankPayoutRepository;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.Sleeper;
import com.casestudies.payoutdelivery.fakebank.bank.domain.aggregate.BankPayout;
import com.casestudies.payoutdelivery.fakebank.bank.domain.exception.SimulatedBankFailureException;
import com.casestudies.payoutdelivery.fakebank.bank.domain.exception.SimulatedBankTimeoutException;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehavior;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehaviorMode;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankPayoutStatus;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.Money;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.PayoutId;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.WorkerId;

class CreateBankPayoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final Duration DELAY = Duration.ofSeconds(5);

    private final List<BankPayout> saved = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    private final BankPayoutRepository repository = payout -> {
        saved.add(payout);
        events.add("saved");
    };
    private final Sleeper sleeper = duration -> events.add("slept " + duration);
    private final InMemoryBankBehaviorStore behaviors = new InMemoryBankBehaviorStore(BankBehavior.normal(DELAY));
    private final CreateBankPayoutService service =
            new CreateBankPayoutService(repository, behaviors, sleeper, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsAndStoresABankPayout() {
        BankPayout created = service.create(command("worker-b"));

        assertThat(saved).containsExactly(created);
        assertThat(created.payoutId()).isEqualTo(PayoutId.of("PO-9001"));
        assertThat(created.workerId()).isEqualTo(WorkerId.of("worker-b"));
        assertThat(created.receivedAt()).isEqualTo(NOW);
        assertThat(created.status()).isEqualTo(BankPayoutStatus.CREATED);
        assertThat(events).containsExactly("saved");
    }

    @Test
    void theSamePayoutSentTwiceCreatesTwoBankPayouts() {
        BankPayout first = service.create(command("worker-b"));
        BankPayout second = service.create(command("worker-a"));

        assertThat(saved).hasSize(2);
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.payoutId()).isEqualTo(second.payoutId());
    }

    @Test
    void delayResponseWaitsBeforeProcessing() {
        behave(BankBehaviorMode.DELAY_RESPONSE);

        BankPayout created = service.create(command("worker-b"));

        assertThat(saved).containsExactly(created);
        assertThat(events).containsExactly("slept PT5S", "saved");
    }

    @Test
    void processThenDelayResponseStoresThePayoutBeforeWaiting() {
        behave(BankBehaviorMode.PROCESS_THEN_DELAY_RESPONSE);

        BankPayout created = service.create(command("worker-b"));

        assertThat(saved).containsExactly(created);
        assertThat(events).containsExactly("saved", "slept PT5S");
    }

    @Test
    void return500FailsWithoutProcessing() {
        behave(BankBehaviorMode.RETURN_500);

        assertThatThrownBy(() -> service.create(command("worker-b")))
                .isInstanceOf(SimulatedBankFailureException.class);
        assertThat(events).isEmpty();
    }

    @Test
    void timeoutWaitsThenFailsWithoutProcessing() {
        behave(BankBehaviorMode.TIMEOUT);

        assertThatThrownBy(() -> service.create(command("worker-b")))
                .isInstanceOf(SimulatedBankTimeoutException.class);
        assertThat(events).containsExactly("slept PT5S");
    }

    private void behave(BankBehaviorMode mode) {
        behaviors.replace(new BankBehavior(mode, DELAY));
    }

    private static CreateBankPayoutCommand command(String workerId) {
        return new CreateBankPayoutCommand(PayoutId.of("PO-9001"), MerchantId.of("M-1001"),
                Money.of("252000", "EGP"), WorkerId.of(workerId));
    }
}
