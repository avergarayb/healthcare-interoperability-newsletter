package com.healthcare.interoperability.fhirsearch.fhir;

import java.util.List;

public record FhirBundle(
        String resourceType,
        String type,
        Integer total,
        List<FhirBundleEntry> entry,
        List<FhirBundleLink> link
) {
}
