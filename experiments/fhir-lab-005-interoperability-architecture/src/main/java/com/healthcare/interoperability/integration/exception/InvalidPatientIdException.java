package com.healthcare.interoperability.integration.exception;

public class InvalidPatientIdException extends RuntimeException {

    public InvalidPatientIdException() {
        super("Patient id must not be blank");
    }
}
