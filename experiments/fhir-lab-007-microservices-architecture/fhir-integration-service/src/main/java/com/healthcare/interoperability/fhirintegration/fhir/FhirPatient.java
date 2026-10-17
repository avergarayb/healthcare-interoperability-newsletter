package com.healthcare.interoperability.fhirintegration.fhir;

import java.util.List;

public record FhirPatient(
        String resourceType,
        String id,
        List<FhirHumanName> name,
        String gender,
        String birthDate
) {
}
