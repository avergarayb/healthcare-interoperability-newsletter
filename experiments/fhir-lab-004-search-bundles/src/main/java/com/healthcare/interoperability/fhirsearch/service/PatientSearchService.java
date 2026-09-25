package com.healthcare.interoperability.fhirsearch.service;

import com.healthcare.interoperability.fhirsearch.client.FhirPatientSearchClient;
import com.healthcare.interoperability.fhirsearch.dto.BundleLinkResponse;
import com.healthcare.interoperability.fhirsearch.dto.PatientSearchResponse;
import com.healthcare.interoperability.fhirsearch.dto.PatientSummary;
import com.healthcare.interoperability.fhirsearch.exception.InvalidSearchIdentifierException;
import com.healthcare.interoperability.fhirsearch.exception.UnexpectedFhirResponseException;
import com.healthcare.interoperability.fhirsearch.fhir.FhirBundle;
import com.healthcare.interoperability.fhirsearch.fhir.FhirBundleEntry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientSearchService {

    private final FhirPatientSearchClient fhirPatientSearchClient;

    public PatientSearchService(FhirPatientSearchClient fhirPatientSearchClient) {
        this.fhirPatientSearchClient = fhirPatientSearchClient;
    }

    public PatientSearchResponse searchByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidSearchIdentifierException();
        }
        FhirBundle bundle = fhirPatientSearchClient.searchByIdentifier(identifier);
        if (!"Bundle".equals(bundle.resourceType()) || !"searchset".equals(bundle.type()) || bundle.total() == null) {
            throw new UnexpectedFhirResponseException("FHIR search did not return a searchset Bundle");
        }
        return new PatientSearchResponse(bundle.total(), patients(bundle), links(bundle));
    }

    private static List<PatientSummary> patients(FhirBundle bundle) {
        if (bundle.entry() == null) {
            return List.of();
        }
        return bundle.entry().stream()
                .map(FhirBundleEntry::resource)
                .filter(resource -> resource != null && "Patient".equals(resource.resourceType()))
                .map(resource -> new PatientSummary(
                        resource.resourceType(),
                        resource.id(),
                        resource.name() == null ? List.of() : List.copyOf(resource.name()),
                        resource.gender(),
                        resource.birthDate()
                ))
                .toList();
    }

    private static List<BundleLinkResponse> links(FhirBundle bundle) {
        if (bundle.link() == null) {
            return List.of();
        }
        return bundle.link().stream()
                .map(link -> new BundleLinkResponse(link.relation(), link.url()))
                .toList();
    }
}
