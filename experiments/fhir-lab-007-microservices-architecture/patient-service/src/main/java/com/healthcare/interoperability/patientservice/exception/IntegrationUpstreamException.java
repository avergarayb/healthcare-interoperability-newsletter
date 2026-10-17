package com.healthcare.interoperability.patientservice.exception;

public class IntegrationUpstreamException extends RuntimeException {

    public IntegrationUpstreamException(Throwable cause) {
        super("Patient data is temporarily unavailable", cause);
    }

    public IntegrationUpstreamException() {
        super("Patient data is temporarily unavailable");
    }
}
