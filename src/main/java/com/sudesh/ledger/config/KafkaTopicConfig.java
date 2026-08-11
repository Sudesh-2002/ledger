package com.sudesh.ledger.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ACCOUNT_EVENTS_TOPIC = "account-events";

    @Bean
    public NewTopic accountEventsTopic() {
        // key = accountId → guarantees all events for one account land on the
        // same partition, so per-account ordering is preserved
        return TopicBuilder.name(ACCOUNT_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}