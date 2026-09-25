package com.healthcare.interoperability.fhirsearch.exception;

public class InvalidSearchIdentifierException extends RuntimeException {

    public InvalidSearchIdentifierException() {
        super("identifier must not be blank");
    }
}
