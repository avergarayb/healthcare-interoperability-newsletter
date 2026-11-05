package com.healthcare.interoperability.eventproducer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "events")
public record EventProperties(String topic, int partitions) {
}
