package com.casestudies.payoutdelivery.platform.payout.adapter.out.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
class PayoutReadyTopicConfiguration {

    static final String TOPIC = "payout-ready";

    @Bean
    NewTopic payoutReadyTopic() {
        return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
    }
}
