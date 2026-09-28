package com.healthcare.interoperability.integration.exception;

public class UnexpectedFhirResponseException extends RuntimeException {

    public UnexpectedFhirResponseException(String message) {
        super(message);
    }

    public UnexpectedFhirResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
