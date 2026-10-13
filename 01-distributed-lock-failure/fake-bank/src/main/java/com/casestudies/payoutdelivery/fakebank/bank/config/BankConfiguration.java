package com.casestudies.payoutdelivery.fakebank.bank.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.BankPayoutRepository;
import com.casestudies.payoutdelivery.fakebank.bank.application.service.CreateBankPayoutService;

@Configuration(proxyBeanMethods = false)
class BankConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateBankPayoutService createBankPayoutService(BankPayoutRepository payouts, Clock clock) {
        return new CreateBankPayoutService(payouts, clock);
    }
}
