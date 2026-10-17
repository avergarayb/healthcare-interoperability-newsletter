package com.healthcare.interoperability.fhirintegration.fhir;

import java.util.List;

public record FhirHumanName(String family, List<String> given) {
}
