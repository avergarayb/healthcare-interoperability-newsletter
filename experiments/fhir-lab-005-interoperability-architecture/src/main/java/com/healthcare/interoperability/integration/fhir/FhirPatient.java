package com.healthcare.interoperability.integration.fhir;

import java.util.List;

public record FhirPatient(
        String resourceType,
        String id,
        List<FhirHumanName> name,
        String gender,
        String birthDate
) {
}
