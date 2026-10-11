package com.casestudies.payoutdelivery.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.ConsumerFactory;

import com.casestudies.payoutdelivery.platform.payout.application.port.in.PreparePayoutUseCase;
import com.casestudies.payoutdelivery.platform.payout.application.port.in.PreparedPayout;
import com.casestudies.payoutdelivery.platform.payout.application.port.in.PublishOutboxUseCase;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// The real path: prepare commits a PENDING row, the scheduled poller sends it to payout-ready,
// and only then marks it PUBLISHED. Nothing in the test calls the publisher directly.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "payout.outbox.publisher.enabled=true",
        "payout.outbox.publisher.poll-interval=100ms"})
class OutboxPublisherTest {

    private static final String TOPIC = "payout-ready";

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PreparePayoutUseCase preparePayout;

    @Autowired
    private PublishOutboxUseCase publishOutbox;

    @Autowired
    private ConsumerFactory<String, String> consumers;

    @Autowired
    private JsonMapper json;

    @BeforeEach
    void startEmpty() {
        jdbc.sql("DELETE FROM payout_dispatch_attempts").update();
        jdbc.sql("DELETE FROM outbox_events").update();
        jdbc.sql("DELETE FROM payouts").update();
    }

    @Test
    void aPreparedPayoutReachesPayoutReadyAndIsMarkedPublished() {
        Instant beforePrepare = Instant.now();
        PreparedPayout prepared = preparePayout.prepare();
        UUID eventId = prepared.event().eventId();

        ConsumerRecord<String, String> record = consumeEvent(eventId);

        assertThat(record.key()).isEqualTo("PO-9001");
        JsonNode payload = json.readTree(record.value());
        assertThat(payload.get("eventId").asString()).isEqualTo(eventId.toString());
        assertThat(payload.get("payoutId").asString()).isEqualTo("PO-9001");
        assertThat(payload.get("merchantId").asString()).isEqualTo("M-1001");
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo(new BigDecimal("252000"));
        assertThat(payload.get("currency").asString()).isEqualTo("EGP");
        assertThat(payload.get("occurredAt").asString()).isNotBlank();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var row = jdbc.sql("SELECT status, published_at FROM outbox_events WHERE id = :id")
                    .param("id", eventId).query().singleRow();
            assertThat(row.get("status")).isEqualTo("PUBLISHED");
            assertThat(((Timestamp) row.get("published_at")).toInstant()).isAfterOrEqualTo(beforePrepare);
        });

        assertThat(publishOutbox.publishPending()).as("a published event is not picked up again").isZero();
    }

    // A fresh group reading from the earliest offset, so the test cannot miss a message sent
    // before it subscribed.
    private ConsumerRecord<String, String> consumeEvent(UUID eventId) {
        Properties overrides = new Properties();
        overrides.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer =
                     consumers.createConsumer("outbox-publisher-test-" + UUID.randomUUID(), null, null, overrides)) {
            consumer.subscribe(List.of(TOPIC));
            Instant deadline = Instant.now().plusSeconds(15);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.value().contains(eventId.toString())) {
                        return record;
                    }
                }
            }
        }
        throw new AssertionError("event " + eventId + " never reached " + TOPIC);
    }
}
