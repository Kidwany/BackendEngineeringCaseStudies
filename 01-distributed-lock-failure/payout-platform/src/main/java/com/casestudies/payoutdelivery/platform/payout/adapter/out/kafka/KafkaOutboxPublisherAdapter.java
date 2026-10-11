package com.casestudies.payoutdelivery.platform.payout.adapter.out.kafka;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxMessage;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxPublisherPort;

@Component
class KafkaOutboxPublisherAdapter implements OutboxPublisherPort {

    private static final Duration ACK_TIMEOUT = Duration.ofSeconds(10);

    private final KafkaTemplate<String, String> kafka;

    KafkaOutboxPublisherAdapter(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(OutboxMessage message) {
        var record = new ProducerRecord<>(PayoutReadyTopicConfiguration.TOPIC, message.aggregateId(), message.payload());
        try {
            kafka.send(record).get(ACK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted publishing outbox event " + message.eventId(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("could not publish outbox event " + message.eventId(), e);
        }
    }
}
