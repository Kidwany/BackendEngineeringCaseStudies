package com.casestudies.payoutdelivery.platform.payout.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.casestudies.payoutdelivery.platform.payout.application.port.out.PayoutRepository;
import com.casestudies.payoutdelivery.platform.payout.application.service.ScenarioPayoutService;

@Configuration(proxyBeanMethods = false)
class PayoutConfiguration {

    @Bean
    ScenarioPayoutService scenarioPayoutService(PayoutRepository payouts) {
        return new ScenarioPayoutService(payouts);
    }
}
