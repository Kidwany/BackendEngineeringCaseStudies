package com.casestudies.payoutdelivery.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.casestudies.payoutdelivery.worker.dispatch.application.port.in.HandlePayoutReadyUseCase;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.in.PayoutReadyCommand;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.MerchantId;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.Money;
import com.casestudies.payoutdelivery.worker.dispatch.domain.valueobject.PayoutId;

// Publishes to payout-ready the way the platform's outbox does, and checks what reaches the use case.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PayoutReadyConsumerTest {

    private static final String TOPIC = "payout-ready";
    private static final long WAIT_MILLIS = 20_000;

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @MockitoSpyBean
    private HandlePayoutReadyUseCase handlePayoutReady;

    @Test
    void consumesAPayoutReadyEventWithAllItsFields() throws Exception {
        UUID eventId = UUID.randomUUID();
        kafka.send(TOPIC, "PO-9001", payoutReady(eventId)).get();

        ArgumentCaptor<PayoutReadyCommand> received = ArgumentCaptor.forClass(PayoutReadyCommand.class);
        verify(handlePayoutReady, timeout(WAIT_MILLIS)).handle(received.capture());

        PayoutReadyCommand command = received.getValue();
        assertThat(command.eventId()).isEqualTo(eventId);
        assertThat(command.payoutId()).isEqualTo(PayoutId.of("PO-9001"));
        assertThat(command.merchantId()).isEqualTo(MerchantId.of("M-1001"));
        assertThat(command.amount()).isEqualTo(Money.of("252000", "EGP"));
        assertThat(command.occurredAt()).isEqualTo(Instant.parse("2026-10-11T10:00:00Z"));
    }

    @Test
    void skipsAMalformedEventAndKeepsConsumingAfterIt() throws Exception {
        UUID eventId = UUID.randomUUID();
        kafka.send(TOPIC, "PO-9001", "{not json").get();
        kafka.send(TOPIC, "PO-9001", "{\"eventId\": \"" + UUID.randomUUID() + "\", \"payoutId\": \"PO-9001\"}").get();
        kafka.send(TOPIC, "PO-9001", payoutReady(eventId)).get();

        ArgumentCaptor<PayoutReadyCommand> received = ArgumentCaptor.forClass(PayoutReadyCommand.class);
        verify(handlePayoutReady, timeout(WAIT_MILLIS)).handle(received.capture());
        assertThat(received.getValue().eventId()).isEqualTo(eventId);
        verify(handlePayoutReady, times(1)).handle(any());
    }

    // The payload as Postgres returns the outbox row's JSONB, plus a field this side does not know.
    private static String payoutReady(UUID eventId) {
        return """
                {"amount": 252000.00, "currency": "EGP", "eventId": "%s", "payoutId": "PO-9001", \
                "merchantId": "M-1001", "occurredAt": "2026-10-11T10:00:00Z", "addedLater": true}"""
                .formatted(eventId);
    }
}
