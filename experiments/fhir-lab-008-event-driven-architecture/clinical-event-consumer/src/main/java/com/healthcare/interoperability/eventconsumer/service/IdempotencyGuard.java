package com.healthcare.interoperability.eventconsumer.service;

import org.springframework.stereotype.Component;

@Component
public class IdempotencyGuard {

    private final ProcessedObservationStore store;

    public IdempotencyGuard(ProcessedObservationStore store) {
        this.store = store;
    }

    public boolean isDuplicate(String eventId) {
        return store.contains(eventId);
    }
}
