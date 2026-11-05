package com.healthcare.interoperability.eventconsumer.fhir;

public record FhirObservation(
        String resourceType,
        String id,
        String status,
        FhirCodeableConcept code,
        FhirReference subject,
        String effectiveDateTime,
        FhirQuantity valueQuantity
) {
}
