package com.healthcare.interoperability.eventproducer.messaging;

import java.time.Instant;

public record ObservationAvailableEvent(
        String eventId,
        String eventType,
        String resourceType,
        String resourceId,
        Instant occurredAt,
        String correlationId
) {
}
