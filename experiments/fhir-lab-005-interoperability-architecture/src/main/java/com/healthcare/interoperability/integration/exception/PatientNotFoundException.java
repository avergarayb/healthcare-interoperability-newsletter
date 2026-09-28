package com.healthcare.interoperability.integration.exception;

public class PatientNotFoundException extends RuntimeException {

    private final String patientId;

    public PatientNotFoundException(String patientId) {
        super("Patient not found: " + patientId);
        this.patientId = patientId;
    }

    public String getPatientId() {
        return patientId;
    }
}
