package com.healthcare.interoperability.fhirclient.controller;

import com.healthcare.interoperability.fhirclient.dto.ErrorResponse;
import com.healthcare.interoperability.fhirclient.exception.FhirClientException;
import com.healthcare.interoperability.fhirclient.exception.FhirResourceNotFoundException;
import com.healthcare.interoperability.fhirclient.exception.InvalidPatientIdException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidPatientIdException.class)
    public ResponseEntity<ErrorResponse> handleInvalidId() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("invalid_id", "Patient id must not be blank"));
    }

    @ExceptionHandler(FhirResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(FhirResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("not_found", "Patient " + ex.getResourceId() + " was not found"));
    }

    @ExceptionHandler(FhirClientException.class)
    public ResponseEntity<ErrorResponse> handleClient(FhirClientException ex) {
        if (ex.isUnavailable()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("upstream_unavailable", "FHIR server is not reachable"));
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("upstream_error", "FHIR server returned an unexpected error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("internal_error", "An unexpected error occurred"));
    }
}
