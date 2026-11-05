package com.healthcare.interoperability.eventproducer.api;

public record PublishObservationResponse(
        String eventId,
        String eventType,
        String resourceType,
        String resourceId,
        String status
) {
}
