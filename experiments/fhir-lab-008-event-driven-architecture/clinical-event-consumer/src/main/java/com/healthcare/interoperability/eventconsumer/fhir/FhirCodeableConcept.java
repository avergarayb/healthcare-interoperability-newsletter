package com.healthcare.interoperability.eventconsumer.fhir;

import java.util.List;

public record FhirCodeableConcept(List<FhirCoding> coding, String text) {
}
