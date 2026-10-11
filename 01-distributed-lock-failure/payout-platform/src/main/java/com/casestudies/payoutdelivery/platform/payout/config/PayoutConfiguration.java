package com.casestudies.payoutdelivery.platform.payout.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxPublisherPort;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxRepository;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.PayoutRepository;
import com.casestudies.payoutdelivery.platform.payout.application.service.PreparePayoutService;
import com.casestudies.payoutdelivery.platform.payout.application.service.PublishOutboxService;
import com.casestudies.payoutdelivery.platform.payout.application.service.ScenarioPayoutService;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class PayoutConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ScenarioPayoutService scenarioPayoutService(PayoutRepository payouts) {
        return new ScenarioPayoutService(payouts);
    }

    @Bean
    PreparePayoutService preparePayoutService(PayoutRepository payouts, OutboxRepository outbox, Clock clock) {
        return new PreparePayoutService(payouts, outbox, clock);
    }

    @Bean
    PublishOutboxService publishOutboxService(OutboxRepository outbox, OutboxPublisherPort publisher, Clock clock,
                                              @Value("${payout.outbox.publisher.batch-size}") int batchSize) {
        return new PublishOutboxService(outbox, publisher, clock, batchSize);
    }
}
