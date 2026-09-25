package com.healthcare.interoperability.fhirsearch.fhir;

import com.healthcare.interoperability.fhirsearch.dto.HumanNameResponse;

import java.util.List;

public record FhirPatientResource(
        String resourceType,
        String id,
        List<HumanNameResponse> name,
        String gender,
        String birthDate
) {
}
