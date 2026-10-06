package com.healthcare.interoperability.profiles.exception;

import com.healthcare.interoperability.profiles.dto.ValidationError;

import java.util.List;

public class ProfileConstraintException extends RuntimeException {

    private final List<ValidationError> errors;

    public ProfileConstraintException(List<ValidationError> errors) {
        super("Patient does not conform to the lab profile");
        this.errors = List.copyOf(errors);
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
