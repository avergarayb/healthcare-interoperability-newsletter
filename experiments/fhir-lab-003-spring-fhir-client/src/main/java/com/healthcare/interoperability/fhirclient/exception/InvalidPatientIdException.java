package com.healthcare.interoperability.fhirclient.exception;

public class InvalidPatientIdException extends RuntimeException {

    public InvalidPatientIdException() {
        super("Patient id must not be blank");
    }
}
