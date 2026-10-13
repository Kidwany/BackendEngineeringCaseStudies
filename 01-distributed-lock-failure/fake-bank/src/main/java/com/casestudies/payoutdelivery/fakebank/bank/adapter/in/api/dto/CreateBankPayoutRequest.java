package com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateBankPayoutRequest(
        @NotBlank @Size(max = 64) String payoutId,
        @NotBlank @Size(max = 64) String merchantId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency) {
}
