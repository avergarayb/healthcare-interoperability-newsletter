package com.healthcare.interoperability.fhirsearch.dto;

import java.util.List;

public record PatientSummary(
        String resourceType,
        String id,
        List<HumanNameResponse> name,
        String gender,
        String birthDate
) {
}
