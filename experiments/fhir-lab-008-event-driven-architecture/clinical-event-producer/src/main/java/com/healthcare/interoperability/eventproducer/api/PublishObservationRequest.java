package com.healthcare.interoperability.eventproducer.api;

public record PublishObservationRequest(String resourceId, String correlationId) {
}
