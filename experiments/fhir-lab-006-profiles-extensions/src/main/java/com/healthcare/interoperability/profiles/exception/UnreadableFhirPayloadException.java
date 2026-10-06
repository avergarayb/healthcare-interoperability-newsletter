package com.healthcare.interoperability.profiles.exception;

public class UnreadableFhirPayloadException extends RuntimeException {

    public UnreadableFhirPayloadException() {
        super("Request body is not readable FHIR JSON");
    }
}
