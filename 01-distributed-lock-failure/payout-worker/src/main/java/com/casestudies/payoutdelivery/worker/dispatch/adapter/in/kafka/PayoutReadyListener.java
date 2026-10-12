package com.casestudies.payoutdelivery.worker.dispatch.adapter.in.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.casestudies.payoutdelivery.worker.dispatch.application.port.in.HandlePayoutReadyUseCase;
import com.casestudies.payoutdelivery.worker.dispatch.application.port.in.PayoutReadyCommand;

import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
class PayoutReadyListener {

    static final String TOPIC = "payout-ready";

    private final HandlePayoutReadyUseCase handlePayoutReady;
    private final JsonMapper json;

    PayoutReadyListener(HandlePayoutReadyUseCase handlePayoutReady, JsonMapper json) {
        this.handlePayoutReady = handlePayoutReady;
        this.json = json;
    }

    @KafkaListener(topics = TOPIC)
    void onPayoutReady(ConsumerRecord<String, String> record) {
        PayoutReadyCommand command;
        try {
            command = json.readValue(record.value(), PayoutReadyMessage.class).toCommand();
        } catch (RuntimeException e) {
            log.error("Skipping malformed PayoutReady at {}-{}@{}: {}",
                    record.topic(), record.partition(), record.offset(), record.value(), e);
            return;
        }
        handlePayoutReady.handle(command);
    }
}
