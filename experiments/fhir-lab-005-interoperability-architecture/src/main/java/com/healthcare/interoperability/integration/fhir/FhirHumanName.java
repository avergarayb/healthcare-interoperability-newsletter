package com.healthcare.interoperability.integration.fhir;

import java.util.List;

public record FhirHumanName(String family, List<String> given) {
}
