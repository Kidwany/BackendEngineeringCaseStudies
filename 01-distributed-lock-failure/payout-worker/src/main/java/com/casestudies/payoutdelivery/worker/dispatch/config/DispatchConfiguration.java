package com.casestudies.payoutdelivery.worker.dispatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.casestudies.payoutdelivery.worker.dispatch.application.service.HandlePayoutReadyService;

@Configuration(proxyBeanMethods = false)
class DispatchConfiguration {

    @Bean
    HandlePayoutReadyService handlePayoutReadyService() {
        return new HandlePayoutReadyService();
    }
}
