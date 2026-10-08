package com.casestudies.payoutdelivery.platform.payout.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.casestudies.payoutdelivery.platform.payout.application.port.out.OutboxRepository;
import com.casestudies.payoutdelivery.platform.payout.application.port.out.PayoutRepository;
import com.casestudies.payoutdelivery.platform.payout.application.service.PreparePayoutService;
import com.casestudies.payoutdelivery.platform.payout.application.service.ScenarioPayoutService;

@Configuration(proxyBeanMethods = false)
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
}
