package com.casestudies.payoutdelivery.fakebank.bank.application.service;

import java.time.Clock;
import java.time.Instant;

import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.Command.CreateBankPayoutCommand;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.CreateBankPayoutUseCase;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.BankBehaviorStore;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.BankPayoutRepository;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.Sleeper;
import com.casestudies.payoutdelivery.fakebank.bank.domain.aggregate.BankPayout;
import com.casestudies.payoutdelivery.fakebank.bank.domain.exception.SimulatedBankFailureException;
import com.casestudies.payoutdelivery.fakebank.bank.domain.exception.SimulatedBankTimeoutException;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehavior;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankPayoutId;

// Not @Transactional on purpose: the single save commits on its own, so PROCESS_THEN_DELAY_RESPONSE
// holds back the response of a payout that is already committed.
public class CreateBankPayoutService implements CreateBankPayoutUseCase {

    private final BankPayoutRepository payouts;
    private final BankBehaviorStore behaviors;
    private final Sleeper sleeper;
    private final Clock clock;

    public CreateBankPayoutService(BankPayoutRepository payouts, BankBehaviorStore behaviors, Sleeper sleeper,
                                   Clock clock) {
        this.payouts = payouts;
        this.behaviors = behaviors;
        this.sleeper = sleeper;
        this.clock = clock;
    }

    @Override
    public BankPayout create(CreateBankPayoutCommand command) {
        Instant receivedAt = clock.instant();
        BankBehavior behavior = behaviors.current();
        return switch (behavior.mode()) {
            case NORMAL -> process(command, receivedAt);
            case DELAY_RESPONSE -> {
                sleeper.sleep(behavior.delay());
                yield process(command, receivedAt);
            }
            case PROCESS_THEN_DELAY_RESPONSE -> {
                BankPayout payout = process(command, receivedAt);
                sleeper.sleep(behavior.delay());
                yield payout;
            }
            case RETURN_500 -> throw new SimulatedBankFailureException();
            case TIMEOUT -> {
                sleeper.sleep(behavior.delay());
                throw new SimulatedBankTimeoutException(behavior.delay());
            }
        };
    }

    private BankPayout process(CreateBankPayoutCommand command, Instant receivedAt) {
        BankPayout payout = BankPayout.create(BankPayoutId.newId(), command.payoutId(), command.merchantId(),
                command.amount(), command.workerId(), receivedAt);
        payouts.save(payout);
        return payout;
    }
}
