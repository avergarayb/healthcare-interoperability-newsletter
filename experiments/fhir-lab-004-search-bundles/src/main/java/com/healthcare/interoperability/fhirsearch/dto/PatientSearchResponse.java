package com.healthcare.interoperability.fhirsearch.dto;

import java.util.List;

public record PatientSearchResponse(
        Integer total,
        List<PatientSummary> patients,
        List<BundleLinkResponse> links
) {
}
