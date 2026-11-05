package com.healthcare.interoperability.eventproducer.api;

import com.healthcare.interoperability.eventproducer.exception.BrokerUnavailableException;
import com.healthcare.interoperability.eventproducer.exception.InvalidResourceIdException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({InvalidResourceIdException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> invalidResourceId(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("invalid_resource_id", "Observation resource id must not be blank"));
    }

    @ExceptionHandler(BrokerUnavailableException.class)
    ResponseEntity<ErrorResponse> brokerUnavailable(BrokerUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("broker_unavailable", "Event broker is not reachable"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("internal_error", "An unexpected error occurred"));
    }
}
