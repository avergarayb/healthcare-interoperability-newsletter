package com.healthcare.interoperability.patientservice.dto;

public record PatientResponse(
        String id,
        String firstName,
        String lastName,
        String gender,
        String birthDate
) {
}
