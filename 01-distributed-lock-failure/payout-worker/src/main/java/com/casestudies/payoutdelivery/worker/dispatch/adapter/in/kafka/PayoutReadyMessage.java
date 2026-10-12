package com.casestudies.payoutdelivery.worker.dispatch.adapter.in.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.casestudies.payoutdelivery.worker.dispatch.application.port.in.PayoutReadyCommand;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.Money;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.PayoutId;

record PayoutReadyMessage(UUID eventId, String payoutId, String merchantId, BigDecimal amount, String currency,
                          Instant occurredAt) {

    PayoutReadyCommand toCommand() {
        return new PayoutReadyCommand(
                eventId,
                PayoutId.of(payoutId),
                MerchantId.of(merchantId),
                Money.of(amount.toPlainString(), currency),
                occurredAt);
    }
}
