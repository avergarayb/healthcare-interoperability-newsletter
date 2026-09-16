package com.healthcare.interoperability.fhirclient.dto;

import java.util.List;

public record PatientResponse(
        String resourceType,
        String id,
        List<HumanNameResponse> name,
        String gender,
        String birthDate
) {
}
