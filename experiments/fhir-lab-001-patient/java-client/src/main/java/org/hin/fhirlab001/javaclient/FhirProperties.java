package org.hin.fhirlab001.javaclient;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fhir")
public record FhirProperties(Server server, PatientRef patient, IdentifierRef identifier, String operation) {

    public record Server(String baseUrl) {
    }

    public record PatientRef(String id) {
    }

    public record IdentifierRef(String system, String value) {
    }
}
