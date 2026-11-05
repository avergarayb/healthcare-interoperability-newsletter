package com.healthcare.interoperability.eventconsumer.model;

public record ProcessedObservation(
        String eventId,
        String observationId,
        String status,
        String code,
        String display,
        String value,
        String unit,
        String patientReference,
        String correlationId
) {
}
