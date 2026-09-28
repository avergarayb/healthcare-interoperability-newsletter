package com.healthcare.interoperability.integration.dto;

public record HealthcarePatient(
        String id,
        String firstName,
        String lastName,
        String gender,
        String birthDate
) {
}
