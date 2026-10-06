package com.healthcare.interoperability.profiles.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidatedHealthcarePatient(
        String id,
        String firstName,
        String lastName,
        String birthDate,
        String preferredContactChannel
) {
}
