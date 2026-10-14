package com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto;

import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehaviorMode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeBankBehaviorRequest(
        @NotNull BankBehaviorMode mode,
        @PositiveOrZero @Max(120_000) Long delayMillis) {
}
