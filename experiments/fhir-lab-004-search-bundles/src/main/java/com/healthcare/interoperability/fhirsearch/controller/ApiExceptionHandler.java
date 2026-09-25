package com.healthcare.interoperability.fhirsearch.controller;

import com.healthcare.interoperability.fhirsearch.dto.ErrorResponse;
import com.healthcare.interoperability.fhirsearch.exception.FhirClientException;
import com.healthcare.interoperability.fhirsearch.exception.InvalidSearchIdentifierException;
import com.healthcare.interoperability.fhirsearch.exception.UnexpectedFhirResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidSearchIdentifierException.class)
    public ResponseEntity<ErrorResponse> handleInvalidIdentifier() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("invalid_identifier", "identifier must not be blank"));
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

    @ExceptionHandler({UnexpectedFhirResponseException.class, Exception.class})
    public ResponseEntity<ErrorResponse> handleUnexpected() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("internal_error", "An unexpected error occurred"));
    }
}
