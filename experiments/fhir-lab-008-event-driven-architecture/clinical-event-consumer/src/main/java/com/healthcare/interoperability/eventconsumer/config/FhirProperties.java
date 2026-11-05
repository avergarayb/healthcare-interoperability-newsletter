package com.healthcare.interoperability.eventconsumer.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fhir")
public record FhirProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
