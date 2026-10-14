package com.casestudies.payoutdelivery.fakebank.bank.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.casestudies.payoutdelivery.fakebank.bank.adapter.out.memory.InMemoryBankBehaviorStore;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.BankBehaviorStore;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.BankPayoutRepository;
import com.casestudies.payoutdelivery.fakebank.bank.application.port.out.Sleeper;
import com.casestudies.payoutdelivery.fakebank.bank.application.service.BankBehaviorService;
import com.casestudies.payoutdelivery.fakebank.bank.application.service.CreateBankPayoutService;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehavior;
import com.casestudies.payoutdelivery.fakebank.bank.domain.valueobject.BankBehaviorMode;

@Configuration(proxyBeanMethods = false)
class BankConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    Sleeper sleeper() {
        return duration -> {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted during a simulated bank delay", e);
            }
        };
    }

    @Bean
    BankBehaviorStore bankBehaviorStore(@Value("${fake-bank.behavior.mode}") BankBehaviorMode mode,
                                        @Value("${fake-bank.behavior.delay}") Duration delay) {
        return new InMemoryBankBehaviorStore(new BankBehavior(mode, delay));
    }

    @Bean
    BankBehaviorService bankBehaviorService(BankBehaviorStore behaviors) {
        return new BankBehaviorService(behaviors);
    }

    @Bean
    CreateBankPayoutService createBankPayoutService(BankPayoutRepository payouts, BankBehaviorStore behaviors,
                                                    Sleeper sleeper, Clock clock) {
        return new CreateBankPayoutService(payouts, behaviors, sleeper, clock);
    }
}
