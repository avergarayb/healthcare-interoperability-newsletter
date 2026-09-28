package com.healthcare.interoperability.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fhir")
public record FhirProperties(String baseUrl) {
}
