package com.healthcare.interoperability.patientservice.controller;

import com.healthcare.interoperability.patientservice.dto.ErrorResponse;
import com.healthcare.interoperability.patientservice.exception.IntegrationUnavailableException;
import com.healthcare.interoperability.patientservice.exception.IntegrationUpstreamException;
import com.healthcare.interoperability.patientservice.exception.InvalidPatientIdException;
import com.healthcare.interoperability.patientservice.exception.PatientNotFoundException;
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

    @ExceptionHandler(IntegrationUnavailableException.class)
    ResponseEntity<ErrorResponse> integrationUnavailable(IntegrationUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("integration_unavailable", "Integration service is not reachable"));
    }

    @ExceptionHandler(IntegrationUpstreamException.class)
    ResponseEntity<ErrorResponse> integrationUpstream(IntegrationUpstreamException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("upstream_error", "Patient data is temporarily unavailable"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("internal_error", "An unexpected error occurred"));
    }
}
