package com.healthcare.interoperability.fhirintegration.dto;

public record IntegrationPatientResponse(
        String id,
        String firstName,
        String lastName,
        String gender,
        String birthDate
) {
}
