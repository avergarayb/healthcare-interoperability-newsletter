package com.healthcare.interoperability.profiles.dto;

public record ValidationSuccessResponse(boolean valid, ValidatedHealthcarePatient patient) {
}
