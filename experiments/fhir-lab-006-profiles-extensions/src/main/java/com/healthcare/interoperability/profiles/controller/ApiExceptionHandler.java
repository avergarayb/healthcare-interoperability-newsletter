package com.healthcare.interoperability.profiles.controller;

import com.healthcare.interoperability.profiles.dto.ErrorResponse;
import com.healthcare.interoperability.profiles.dto.ValidationError;
import com.healthcare.interoperability.profiles.dto.ValidationFailureResponse;
import com.healthcare.interoperability.profiles.exception.ProfileConstraintException;
import com.healthcare.interoperability.profiles.exception.UnreadableFhirPayloadException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnreadableFhirPayloadException.class)
    public ResponseEntity<ValidationFailureResponse> handleUnreadable() {
        return unreadable();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ValidationFailureResponse> handleMissingBody() {
        return unreadable();
    }

    @ExceptionHandler(ProfileConstraintException.class)
    public ResponseEntity<ValidationFailureResponse> handleProfile(ProfileConstraintException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ValidationFailureResponse(false, ex.getErrors()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("internal_error", "An unexpected error occurred"));
    }

    private ResponseEntity<ValidationFailureResponse> unreadable() {
        return ResponseEntity.badRequest().body(new ValidationFailureResponse(false, List.of(
                new ValidationError("unreadable_payload", "Request body is not readable FHIR JSON")
        )));
    }
}
