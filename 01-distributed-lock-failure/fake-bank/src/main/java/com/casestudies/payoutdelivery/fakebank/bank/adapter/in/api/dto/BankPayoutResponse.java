package com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BankPayoutResponse(UUID bankPayoutId, String payoutId, String merchantId, BigDecimal amount,
                                 String currency, String workerId, Instant receivedAt, String status) {
}
