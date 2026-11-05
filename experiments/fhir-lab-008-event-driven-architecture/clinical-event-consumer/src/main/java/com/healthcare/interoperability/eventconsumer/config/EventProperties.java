package com.healthcare.interoperability.eventconsumer.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "events")
public record EventProperties(
        String topic,
        String deadLetterTopic,
        String groupId,
        String deadLetterGroupId,
        Duration retryInterval,
        int retryAttempts,
        int partitions
) {
}
