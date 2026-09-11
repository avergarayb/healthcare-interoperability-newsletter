package org.hin.fhirlab001.javaclient;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fhir")
public record FhirProperties(Server server, PatientRef patient) {

    public record Server(String baseUrl) {
    }

    public record PatientRef(String id) {
    }
}
