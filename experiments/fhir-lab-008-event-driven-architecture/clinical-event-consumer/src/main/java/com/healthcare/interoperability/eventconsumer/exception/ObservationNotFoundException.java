package com.healthcare.interoperability.eventconsumer.exception;

public class ObservationNotFoundException extends RuntimeException {

    public ObservationNotFoundException(String observationId) {
        super("Observation " + observationId + " was not found");
    }
}
