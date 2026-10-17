package com.healthcare.interoperability.fhirintegration.exception;

public class InvalidPatientIdException extends RuntimeException {

    public InvalidPatientIdException() {
        super("Patient id must not be blank");
    }
}
