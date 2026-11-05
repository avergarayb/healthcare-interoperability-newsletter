package com.healthcare.interoperability.eventproducer.exception;

public class InvalidResourceIdException extends RuntimeException {

    public InvalidResourceIdException() {
        super("Observation resource id must not be blank");
    }
}
