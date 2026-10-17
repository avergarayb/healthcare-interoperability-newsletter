package com.healthcare.interoperability.fhirintegration.controller;

import com.healthcare.interoperability.fhirintegration.dto.ErrorResponse;
import com.healthcare.interoperability.fhirintegration.exception.FhirClientException;
import com.healthcare.interoperability.fhirintegration.exception.InvalidPatientIdException;
import com.healthcare.interoperability.fhirintegration.exception.PatientNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidPatientIdException.class)
    ResponseEntity<ErrorResponse> invalidId(InvalidPatientIdException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("invalid_id", "Patient id must not be blank"));
    }

    @ExceptionHandler(PatientNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(PatientNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("not_found", "Patient " + exception.patientId() + " was not found"));
    }

    @ExceptionHandler(FhirClientException.class)
    ResponseEntity<ErrorResponse> fhirFailure(FhirClientException exception) {
        return switch (exception.kind()) {
            case UNAVAILABLE -> ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("upstream_unavailable", "FHIR server is not reachable"));
            case UNREADABLE -> ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("unreadable_payload", "FHIR response could not be read"));
            case UPSTREAM_HTTP -> ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("upstream_error", "FHIR server returned an unexpected error"));
        };
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("internal_error", "An unexpected error occurred"));
    }
}
