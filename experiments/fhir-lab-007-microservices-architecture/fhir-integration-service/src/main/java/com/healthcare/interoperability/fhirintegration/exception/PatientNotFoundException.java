package com.healthcare.interoperability.fhirintegration.exception;

public class PatientNotFoundException extends RuntimeException {

    private final String patientId;

    public PatientNotFoundException(String patientId) {
        super("Patient " + patientId + " was not found");
        this.patientId = patientId;
    }

    public String patientId() {
        return patientId;
    }
}
