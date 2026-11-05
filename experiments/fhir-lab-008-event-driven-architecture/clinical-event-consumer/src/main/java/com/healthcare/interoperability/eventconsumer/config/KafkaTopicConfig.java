package com.healthcare.interoperability.eventconsumer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties({EventProperties.class, FhirProperties.class})
public class KafkaTopicConfig {

    @Bean
    NewTopic observationAvailableTopic(EventProperties properties) {
        return TopicBuilder.name(properties.topic())
                .partitions(properties.partitions())
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic observationAvailableDeadLetterTopic(EventProperties properties) {
        return TopicBuilder.name(properties.deadLetterTopic())
                .partitions(properties.partitions())
                .replicas(1)
                .build();
    }
}
