package com.healthcare.interoperability.eventconsumer.exception;

public class ProcessedEventNotFoundException extends RuntimeException {

    private final String eventId;

    public ProcessedEventNotFoundException(String eventId) {
        super("Processed event " + eventId + " was not found");
        this.eventId = eventId;
    }

    public String eventId() {
        return eventId;
    }
}
