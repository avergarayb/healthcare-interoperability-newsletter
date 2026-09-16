package com.healthcare.interoperability.fhirclient.exception;

public class FhirResourceNotFoundException extends RuntimeException {

    private final String resourceId;

    public FhirResourceNotFoundException(String resourceId) {
        super("FHIR Patient not found: " + resourceId);
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }
}
