package com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.mapper;

import java.time.Duration;
import java.util.Optional;

import com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto.BankBehaviorResponse;
import com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto.ChangeBankBehaviorRequest;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.in.Command.ChangeBankBehaviorCommand;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehavior;

public final class BankBehaviorApiMapper {

    private BankBehaviorApiMapper() {
    }

    public static ChangeBankBehaviorCommand toCommand(ChangeBankBehaviorRequest request) {
        return new ChangeBankBehaviorCommand(request.mode(),
                Optional.ofNullable(request.delayMillis()).map(Duration::ofMillis));
    }

    public static BankBehaviorResponse toResponse(BankBehavior behavior) {
        return new BankBehaviorResponse(behavior.mode().name(), behavior.delay().toMillis());
    }
}
