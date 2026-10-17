package com.healthcare.interoperability.patientservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration")
public record IntegrationProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
