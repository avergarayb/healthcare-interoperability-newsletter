package com.healthcare.interoperability.fhirsearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fhir")
public record FhirProperties(String baseUrl) {
}
