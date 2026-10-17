package com.healthcare.interoperability.patientservice.exception;

public class IntegrationUnavailableException extends RuntimeException {

    public IntegrationUnavailableException(Throwable cause) {
        super("Integration service is not reachable", cause);
    }
}
