package com.healthcare.interoperability.fhirsearch.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FhirProperties.class)
public class RestClientConfig {

    @Bean
    RestClient fhirRestClient(FhirProperties fhirProperties) {
        return RestClient.builder()
                .baseUrl(fhirProperties.baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/fhir+json")
                .build();
    }
}
